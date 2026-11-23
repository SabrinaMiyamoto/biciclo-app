import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FirebaseApp, initializeApp } from 'firebase/app';
import {
  Auth, GoogleAuthProvider, User, createUserWithEmailAndPassword, getAuth,
  signInWithEmailAndPassword, signInWithPopup, signOut, updateProfile,
} from 'firebase/auth';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiService } from './api.service';
import { LoginResponse, Role } from './models';

const CHAVE = 'biciclo.sessao';

/** Dados extras pedidos no cadastro de parceiro. */
export interface DadosParceiro {
  nomeFantasia: string;
  cnpj: string;
  latitude: number;
  longitude: number;
}

/**
 * Login em duas etapas:
 * 1. o Firebase Auth autentica (Google ou e-mail/senha) e devolve um idToken;
 * 2. a API troca esse idToken pelo JWT dela, que vai em todas as chamadas.
 * A API nunca recebe senha.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService);
  private router = inject(Router);
  private app?: FirebaseApp;
  private auth?: Auth;

  readonly sessao = signal<LoginResponse | null>(this.lerSessao());
  readonly logado = computed(() => this.sessao() !== null);
  readonly role = computed<Role | null>(() => this.sessao()?.role ?? null);

  /** true enquanto environment.ts ainda tem os valores de exemplo. */
  readonly firebaseNaoConfigurado = environment.firebase.apiKey.startsWith('COLE_');

  get token(): string | null {
    return this.sessao()?.token ?? null;
  }

  async entrarComEmail(email: string, senha: string) {
    const cred = await signInWithEmailAndPassword(this.firebase(), email, senha);
    return this.trocarToken(cred.user);
  }

  async entrarComGoogle() {
    const cred = await signInWithPopup(this.firebase(), new GoogleAuthProvider());
    return this.trocarToken(cred.user);
  }

  /** Cria a conta no Firebase e o perfil na API (ciclista ou parceiro). */
  async cadastrarComEmail(nome: string, email: string, senha: string, role: Role, parceiro?: DadosParceiro) {
    const cred = await createUserWithEmailAndPassword(this.firebase(), email, senha);
    await updateProfile(cred.user, { displayName: nome });
    return this.registrar(cred.user, nome, role, parceiro);
  }

  async cadastrarComGoogle(role: Role, parceiro?: DadosParceiro) {
    const cred = await signInWithPopup(this.firebase(), new GoogleAuthProvider());
    return this.registrar(cred.user, cred.user.displayName ?? '', role, parceiro);
  }

  async sair() {
    this.salvar(null);
    if (this.auth) await signOut(this.auth);
    this.router.navigate(['/']);
  }

  /** Chamado pelo interceptor quando a API responde 401. */
  sessaoExpirada() {
    this.salvar(null);
    this.router.navigate(['/entrar'], { queryParams: { expirou: 1 } });
  }

  private async trocarToken(user: User) {
    const idToken = await user.getIdToken();
    const resp = await firstValueFrom(this.api.loginFirebase(idToken));
    this.salvar(resp);
    return resp;
  }

  private async registrar(user: User, nome: string, role: Role, parceiro?: DadosParceiro) {
    const idToken = await user.getIdToken();
    const resp = await firstValueFrom(this.api.registrar({ idToken, role, nome, ...parceiro }));
    this.salvar(resp);
    return resp;
  }

  private firebase(): Auth {
    if (!this.auth) {
      this.app = initializeApp(environment.firebase);
      this.auth = getAuth(this.app);
    }
    return this.auth;
  }

  private salvar(s: LoginResponse | null) {
    try {
      if (s) localStorage.setItem(CHAVE, JSON.stringify(s));
      else localStorage.removeItem(CHAVE);
    } catch { /* navegador sem localStorage: a sessão fica só na memória */ }
    this.sessao.set(s);
  }

  private lerSessao(): LoginResponse | null {
    try {
      const s = JSON.parse(localStorage.getItem(CHAVE) ?? 'null') as LoginResponse | null;
      return s && !tokenExpirado(s.token) ? s : null;
    } catch {
      return null;
    }
  }
}

/** Lê o campo exp do JWT (sem validar a assinatura, isso é papel da API). */
export function tokenExpirado(jwt: string): boolean {
  try {
    const payload = JSON.parse(atob(jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}
