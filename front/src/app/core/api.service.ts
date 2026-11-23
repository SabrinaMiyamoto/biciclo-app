import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import {
  Alerta, AlertaRequest, Cupom, Extrato, ImportacaoOsm, LoginResponse, Oficina, Poi, PoiRequest,
  PontoTrajeto, Recompensa, RecompensaRequest, RegisterRequest, Role, StatusAlerta, Trajeto, Usuario,
} from './models';

/** Todas as chamadas à biciclo-api. O token JWT é colocado pelo authInterceptor. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private url = environment.apiUrl;

  // Autenticação
  loginFirebase(idToken: string) {
    return this.http.post<LoginResponse>(`${this.url}/auth/firebase`, { idToken });
  }
  registrar(body: RegisterRequest) {
    return this.http.post<LoginResponse>(`${this.url}/auth/register`, body);
  }

  // Perfil
  meuPerfil() {
    return this.http.get<Usuario>(`${this.url}/usuarios/me`);
  }
  meuExtrato() {
    return this.http.get<Extrato[]>(`${this.url}/usuarios/me/extrato`);
  }

  // Pontos de interesse (raio em metros, máximo 5000)
  poisProximos(latitude: number, longitude: number, raio = 3000) {
    const params = new HttpParams().set('latitude', latitude).set('longitude', longitude).set('raio', raio);
    return this.http.get<Poi[]>(`${this.url}/pois`, { params });
  }

  /** Oficinas do Google Places (exige login; 503 se a API não tiver chave do Google). */
  oficinasProximas(latitude: number, longitude: number, raio = 3000) {
    const params = new HttpParams().set('latitude', latitude).set('longitude', longitude).set('raio', raio);
    return this.http.get<Oficina[]>(`${this.url}/oficinas`, { params });
  }

  // Trajetos
  registrarTrajeto(pontos: PontoTrajeto[]) {
    return this.http.post<Trajeto>(`${this.url}/trajetos`, { pontos });
  }
  meusTrajetos() {
    return this.http.get<Trajeto[]>(`${this.url}/trajetos/mine`);
  }

  // Recompensas e cupons
  recompensas() {
    return this.http.get<Recompensa[]>(`${this.url}/recompensas`);
  }
  minhasRecompensas() {
    return this.http.get<Recompensa[]>(`${this.url}/recompensas/minhas`);
  }
  criarRecompensa(body: RecompensaRequest) {
    return this.http.post<Recompensa>(`${this.url}/recompensas`, body);
  }
  editarRecompensa(id: string, body: RecompensaRequest) {
    return this.http.put<Recompensa>(`${this.url}/recompensas/${id}`, body);
  }
  desativarRecompensa(id: string) {
    return this.http.delete<void>(`${this.url}/recompensas/${id}`);
  }
  resgatar(id: string) {
    return this.http.post<Cupom>(`${this.url}/recompensas/${id}/resgatar`, {});
  }
  meusCupons() {
    return this.http.get<Cupom[]>(`${this.url}/cupons/meus`);
  }
  validarCupom(codigo: string) {
    return this.http.post<Cupom>(`${this.url}/cupons/validar`, { codigo });
  }

  // Alertas de infraestrutura
  criarAlerta(body: AlertaRequest) {
    return this.http.post<Alerta>(`${this.url}/alertas-infraestrutura`, body);
  }
  meusAlertas() {
    return this.http.get<Alerta[]>(`${this.url}/alertas-infraestrutura/minhas`);
  }

  // Administração (somente ADMIN)
  adminAlertas(status?: StatusAlerta) {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Alerta[]>(`${this.url}/admin/alertas`, { params });
  }
  adminStatusAlerta(id: string, status: StatusAlerta) {
    return this.http.patch<Alerta>(`${this.url}/admin/alertas/${id}/status`, { status });
  }
  adminCriarPoi(body: PoiRequest) {
    return this.http.post<Poi>(`${this.url}/admin/pois`, body);
  }
  adminEditarPoi(id: string, body: PoiRequest) {
    return this.http.put<Poi>(`${this.url}/admin/pois/${id}`, body);
  }
  adminDesativarPoi(id: string) {
    return this.http.delete<void>(`${this.url}/admin/pois/${id}`);
  }
  adminImportarOsm(forcar = false) {
    return this.http.post<ImportacaoOsm>(`${this.url}/admin/pois/importar-osm`, {}, {
      params: new HttpParams().set('forcar', forcar),
    });
  }
  adminUsuarios(role?: Role) {
    const params = role ? new HttpParams().set('role', role) : undefined;
    return this.http.get<Usuario[]>(`${this.url}/admin/usuarios`, { params });
  }
  adminAlterarRole(id: string, role: Role) {
    return this.http.patch<Usuario>(`${this.url}/admin/usuarios/${id}/role`, { role });
  }
}

/** Converte um erro HTTP em uma frase para mostrar na tela. */
export function mensagemErro(e: unknown): string {
  const err = e as { status?: number; error?: { message?: string }; message?: string; code?: string };
  if (err?.code?.startsWith?.('auth/')) return mensagemFirebase(err.code);
  if (err?.status === 0) return 'Não foi possível falar com a API. Ela está rodando em ' + environment.apiUrl + '?';
  if (err?.error?.message) return err.error.message;
  const porStatus: Record<number, string> = {
    400: 'Dados inválidos. Confira os campos.',
    401: 'Sua sessão expirou. Entre novamente.',
    403: 'Seu perfil não tem permissão para isso.',
    404: 'Não encontrado.',
    409: 'Já existe um cadastro com esses dados.',
    422: 'Não foi possível concluir (regra de negócio).',
  };
  return (err?.status && porStatus[err.status]) || err?.message || 'Erro inesperado.';
}

function mensagemFirebase(code: string): string {
  const m: Record<string, string> = {
    'auth/invalid-credential': 'E-mail ou senha incorretos.',
    'auth/email-already-in-use': 'Este e-mail já tem conta. Use "Entrar".',
    'auth/weak-password': 'A senha precisa ter pelo menos 6 caracteres.',
    'auth/invalid-email': 'E-mail inválido.',
    'auth/popup-closed-by-user': 'A janela do Google foi fechada antes de concluir.',
    'auth/unauthorized-domain': 'Adicione localhost em Authentication → Settings → Authorized domains no Firebase.',
    'auth/invalid-api-key': 'Configure a apiKey do Firebase em src/environments/environment.ts.',
    'auth/api-key-not-valid.-please-pass-a-valid-api-key.': 'Configure a apiKey do Firebase em src/environments/environment.ts.',
  };
  return m[code] ?? `Erro no login (${code}).`;
}
