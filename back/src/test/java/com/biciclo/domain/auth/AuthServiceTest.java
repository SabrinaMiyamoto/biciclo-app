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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String ID_TOKEN = "id-token";
    private static final String UID = "uid-1";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ParceiroRepository parceiroRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private FirebaseToken firebaseToken;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("jwt");
    }

    @Test
    void bloqueiaAutocadastroComoAdmin() {
        RegisterRequest request = new RegisterRequest();
        request.setIdToken(ID_TOKEN);
        request.setRole(Role.ADMIN);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verifyNoInteractions(firebaseAuthService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void exigeCnpjParaCadastroDeParceiro() {
        mockFirebaseToken();
        RegisterRequest request = new RegisterRequest();
        request.setIdToken(ID_TOKEN);
        request.setRole(Role.PARTNER);
        request.setNomeFantasia("Pão Quente");
        // cnpj ausente

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastraCiclistaUsandoUidComoIdDoDocumento() {
        mockFirebaseToken();
        RegisterRequest request = new RegisterRequest();
        request.setIdToken(ID_TOKEN);
        request.setRole(Role.CYCLIST);
        request.setNome("Ana");

        LoginResponse response = authService.register(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();

        assertEquals(UID, salvo.getId());
        assertEquals(UID, salvo.getFirebaseUid());
        assertEquals("ana@biciclo.com", salvo.getEmail());
        assertEquals("Ana", salvo.getNome());
        assertEquals(Role.CYCLIST, salvo.getRole());
        assertEquals("google.com", salvo.getProvedor());
        assertEquals("https://foto/ana.png", salvo.getFotoUrl());
        assertEquals(0, salvo.getSaldoPontos());
        assertNotNull(salvo.getCreatedAt());

        assertEquals("jwt", response.token());
        assertEquals(UID, response.id());
        assertEquals(Role.CYCLIST, response.role());
    }

    @Test
    void cadastraParceiroComIdIgualAoUid() {
        mockFirebaseToken();
        when(parceiroRepository.existsByCnpj("12.345.678/0001-90")).thenReturn(false);

        RegisterRequest request = new RegisterRequest();
        request.setIdToken(ID_TOKEN);
        request.setRole(Role.PARTNER);
        request.setNome("Padaria Pão Quente");
        request.setNomeFantasia("Pão Quente");
        request.setCnpj("12.345.678/0001-90");
        request.setLatitude(-27.59);
        request.setLongitude(-48.55);

        authService.register(request);

        ArgumentCaptor<Parceiro> captor = ArgumentCaptor.forClass(Parceiro.class);
        verify(parceiroRepository).save(captor.capture());
        Parceiro salvo = captor.getValue();

        assertEquals(UID, salvo.getId());
        assertEquals(UID, salvo.getUsuarioId());
        assertEquals("Pão Quente", salvo.getNomeFantasia());
        assertEquals(-27.59, salvo.getLatitude());
    }

    @Test
    void primeiroLoginCriaCiclistaAutomaticamente() {        mockFirebaseToken();
        when(usuarioRepository.existsByEmail("ana@biciclo.com")).thenReturn(false);

        LoginResponse response = authService.loginComFirebase(ID_TOKEN);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Role.CYCLIST, captor.getValue().getRole());
        assertEquals("jwt", response.token());
        assertEquals(Role.CYCLIST, response.role());
    }

    @Test
    void loginReutilizaContaExistenteSemGravar() {
        mockFirebaseToken();
        Usuario existente = new Usuario();
        existente.setId(UID);
        existente.setFirebaseUid(UID);
        existente.setEmail("ana@biciclo.com");
        existente.setNome("Ana");
        existente.setRole(Role.PARTNER);
        when(usuarioRepository.findById(UID)).thenReturn(Optional.of(existente));

        LoginResponse response = authService.loginComFirebase(ID_TOKEN);

        verify(usuarioRepository, never()).save(any());
        assertEquals(Role.PARTNER, response.role());
    }

    @Test
    void loginPromoveCiclistaComEmailDeAdminConfigurado() {
        mockFirebaseToken();
        Usuario existente = usuarioExistente(Role.CYCLIST);
        when(usuarioRepository.findById(UID)).thenReturn(Optional.of(existente));

        LoginResponse response = comAdmins("ana@biciclo.com").loginComFirebase(ID_TOKEN);

        assertEquals(Role.ADMIN, response.role());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void loginNaoPromoveParceiroMesmoComEmailDeAdmin() {
        mockFirebaseToken();
        when(usuarioRepository.findById(UID)).thenReturn(Optional.of(usuarioExistente(Role.PARTNER)));

        LoginResponse response = comAdmins("ana@biciclo.com").loginComFirebase(ID_TOKEN);

        assertEquals(Role.PARTNER, response.role());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginNaoPromoveEmailForaDaLista() {
        mockFirebaseToken();
        when(usuarioRepository.findById(UID)).thenReturn(Optional.of(usuarioExistente(Role.CYCLIST)));

        LoginResponse response = comAdmins("outra@biciclo.com").loginComFirebase(ID_TOKEN);

        assertEquals(Role.CYCLIST, response.role());
        verify(usuarioRepository, never()).save(any());
    }

    private AuthService comAdmins(String... emails) {
        return new AuthService(usuarioRepository, parceiroRepository, jwtService, firebaseAuthService,
                new AdminProperties(List.of(emails)));
    }

    private static Usuario usuarioExistente(Role role) {
        Usuario usuario = new Usuario();
        usuario.setId(UID);
        usuario.setFirebaseUid(UID);
        usuario.setEmail("ana@biciclo.com");
        usuario.setNome("Ana");
        usuario.setRole(role);
        return usuario;
    }

    @Test
    void loginNaoVinculaContaPorEmailDeOutraOrigem() {
        mockFirebaseToken();
        when(usuarioRepository.existsByEmail("ana@biciclo.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginComFirebase(ID_TOKEN));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejeitaTokenSemEmail() {
        when(firebaseAuthService.verifyIdToken(ID_TOKEN)).thenReturn(firebaseToken);
        lenient().when(firebaseToken.getUid()).thenReturn(UID);
        lenient().when(firebaseToken.getEmail()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginComFirebase(ID_TOKEN));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    private void mockFirebaseToken() {
        when(firebaseAuthService.verifyIdToken(ID_TOKEN)).thenReturn(firebaseToken);
        lenient().when(firebaseToken.getUid()).thenReturn(UID);
        lenient().when(firebaseToken.getEmail()).thenReturn("Ana@Biciclo.com");
        lenient().when(firebaseToken.getName()).thenReturn("Ana");
        lenient().when(firebaseToken.getClaims())
                .thenReturn(Map.of("firebase", Map.of("sign_in_provider", "google.com")));
        lenient().when(firebaseToken.getPicture()).thenReturn("https://foto/ana.png");
    }
}
