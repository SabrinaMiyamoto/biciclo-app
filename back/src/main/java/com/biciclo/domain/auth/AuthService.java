package com.biciclo.domain.auth;

import com.biciclo.common.enums.Role;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.config.AdminProperties;
import com.biciclo.domain.auth.dto.LoginResponse;
import com.biciclo.domain.auth.dto.RegisterRequest;
import com.biciclo.domain.parceiro.Parceiro;
import com.biciclo.domain.parceiro.ParceiroRepository;
import com.biciclo.domain.usuario.Usuario;
import com.biciclo.domain.usuario.UsuarioRepository;
import com.biciclo.security.FirebaseAuthService;
import com.biciclo.security.JwtService;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Autenticação delegada ao Firebase Auth (Google e e-mail/senha).
 *
 * O cliente cria/entra na conta pelo SDK do Firebase, obtém um ID token e o
 * envia para a API. A API valida o token com o Admin SDK, mantém somente o
 * perfil no Firestore e devolve um JWT próprio. A senha nunca trafega pela API.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ParceiroRepository parceiroRepository;
    private final JwtService jwtService;
    private final FirebaseAuthService firebaseAuthService;
    private final AdminProperties adminProperties;

    public AuthService(UsuarioRepository usuarioRepository,
                       ParceiroRepository parceiroRepository,
                       JwtService jwtService,
                       FirebaseAuthService firebaseAuthService,
                       AdminProperties adminProperties) {
        this.usuarioRepository = usuarioRepository;
        this.parceiroRepository = parceiroRepository;
        this.jwtService = jwtService;
        this.firebaseAuthService = firebaseAuthService;
        this.adminProperties = adminProperties == null ? new AdminProperties(null) : adminProperties;
    }

    /**
     * Cadastro do perfil (ciclista ou parceiro). O cliente já criou a conta no
     * Firebase Auth e envia o ID token; aqui validamos o token e gravamos o
     * perfil no Firestore.
     */
    public LoginResponse register(RegisterRequest request) {
        validarRoleAutocadastro(request.getRole());

        FirebaseToken token = firebaseAuthService.verifyIdToken(request.getIdToken());
        String uid = token.getUid();
        String email = requireEmail(token);

        if (usuarioRepository.findById(uid).isPresent()
                || usuarioRepository.findByFirebaseUid(uid).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Usuário já cadastrado");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        // Valida os dados do parceiro antes de persistir qualquer coisa, para
        // não deixar um usuário órfão quando o payload estiver incompleto.
        Parceiro parceiro = request.getRole() == Role.PARTNER
                ? montarParceiro(request, uid)
                : null;

        Usuario usuario = novoUsuario(uid, token, email, request.getRole(), request.getNome());
        usuarioRepository.save(usuario);

        if (parceiro != null) {
            parceiroRepository.save(parceiro);
        }

        return buildResponse(usuario);
    }

    /**
     * Login (ou cadastro automático de ciclista) via ID token do Firebase.
     * Serve tanto para Google quanto para e-mail/senha.
     */
    public LoginResponse loginComFirebase(String idToken) {
        FirebaseToken token = firebaseAuthService.verifyIdToken(idToken);
        String uid = token.getUid();
        String email = requireEmail(token);

        Usuario usuario = localizarPorUid(uid).orElseGet(() -> {
            // Não vinculamos contas automaticamente por e-mail: um cadastro
            // antigo sem e-mail verificado poderia ser sequestrado.
            if (usuarioRepository.existsByEmail(email)) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "E-mail já vinculado a outra conta. Entre com o método original.");
            }
            return criarCiclistaAutomatico(uid, token, email);
        });

        promoverSeAdminConfigurado(usuario);
        return buildResponse(usuario);
    }

    /**
     * Promove a ADMIN o usuário cujo e-mail está em app.admin.emails. É a forma
     * de criar o primeiro administrador, já que o autocadastro não permite ADMIN.
     * Parceiros não são promovidos para não perder o vínculo com o parceiro.
     */
    private void promoverSeAdminConfigurado(Usuario usuario) {
        if (usuario.getRole() == Role.CYCLIST && adminProperties.isAdmin(usuario.getEmail())) {
            usuario.setRole(Role.ADMIN);
            usuarioRepository.save(usuario);
        }
    }

    private Optional<Usuario> localizarPorUid(String uid) {
        Optional<Usuario> porId = usuarioRepository.findById(uid);
        if (porId.isPresent()) {
            return porId;
        }
        return usuarioRepository.findByFirebaseUid(uid);
    }

    private Usuario criarCiclistaAutomatico(String uid, FirebaseToken token, String email) {
        Usuario novo = novoUsuario(uid, token, email, Role.CYCLIST, null);
        usuarioRepository.save(novo);
        return novo;
    }

    private Usuario novoUsuario(String uid,
                                FirebaseToken token,
                                String email,
                                Role role,
                                String nomeInformado) {
        Usuario usuario = new Usuario();
        usuario.setId(uid);
        usuario.setFirebaseUid(uid);
        usuario.setNome(resolverNome(nomeInformado, token));
        usuario.setEmail(email);
        usuario.setRole(role);
        usuario.setProvedor(provedor(token));
        usuario.setFotoUrl(token.getPicture());
        usuario.setSaldoPontos(0);
        usuario.setCreatedAt(new Date());
        return usuario;
    }

    private Parceiro montarParceiro(RegisterRequest request, String usuarioId) {
        String cnpj = requirePartnerField(request.getCnpj(), "cnpj");
        if (parceiroRepository.existsByCnpj(cnpj)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CNPJ já cadastrado");
        }

        Parceiro parceiro = new Parceiro();
        // O documento do parceiro usa o mesmo id do usuário (UID), o que
        // permite referenciá-lo diretamente como `parceiroId` nas recompensas.
        parceiro.setId(usuarioId);
        parceiro.setUsuarioId(usuarioId);
        parceiro.setNomeFantasia(requirePartnerField(request.getNomeFantasia(), "nomeFantasia"));
        parceiro.setCnpj(cnpj);
        parceiro.setLatitude(requireCoordenada(request.getLatitude(), "latitude", 90));
        parceiro.setLongitude(requireCoordenada(request.getLongitude(), "longitude", 180));
        return parceiro;
    }

    private void validarRoleAutocadastro(Role role) {
        // Perfis administrativos são criados por um ADMIN já autenticado.
        if (role != Role.CYCLIST && role != Role.PARTNER) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Perfil não permitido no autocadastro");
        }
    }

    private String requireEmail(FirebaseToken token) {
        if (token.getEmail() == null || token.getEmail().isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token Firebase sem e-mail associado");
        }
        return token.getEmail().trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Extrai o provedor ("google.com", "password", ...) do claim reservado
     * {@code firebase.sign_in_provider}, já que o Admin SDK desta versão não
     * expõe um getter dedicado.
     */
    private String provedor(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        if (claims == null) {
            return null;
        }
        Object firebase = claims.get("firebase");
        if (firebase instanceof Map<?, ?> dados
                && dados.get("sign_in_provider") instanceof String valor) {
            return valor;
        }
        return null;
    }

    private String resolverNome(String nomeInformado, FirebaseToken token) {
        if (nomeInformado != null && !nomeInformado.isBlank()) {
            return nomeInformado.trim();
        }
        if (token.getName() != null && !token.getName().isBlank()) {
            return token.getName();
        }
        return token.getEmail();
    }

    private String requirePartnerField(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Campo '" + field + "' é obrigatório para cadastro de parceiro");
        }
        return value.trim();
    }

    private Double requireCoordenada(Double valor, String campo, int limite) {
        if (valor == null || valor.isNaN() || valor.isInfinite() || Math.abs(valor) > limite) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Campo '" + campo + "' é obrigatório e deve estar entre -" + limite + " e " + limite);
        }
        return valor;
    }

    private LoginResponse buildResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRole());
        return new LoginResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }
}
