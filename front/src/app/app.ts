import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { NOMES_ROLE } from './core/models';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/" (click)="menu.set(false)">
        <span class="brand-pin">🚲</span>
        <span><strong>BICICLO</strong><small>FLORIPA</small></span>
      </a>
      <button class="menu-toggle" (click)="menu.set(!menu())" aria-label="Abrir menu">☰</button>
      <nav [class.open]="menu()" (click)="menu.set(false)">
        <a routerLink="/mapa" routerLinkActive="ativo">Mapa</a>
        <a routerLink="/recompensas" routerLinkActive="ativo">Recompensas</a>
        @switch (auth.role()) {
          @case ('CYCLIST') {
            <a routerLink="/pedalar" routerLinkActive="ativo">Pedalar</a>
            <a routerLink="/alertas" routerLinkActive="ativo">Alertas</a>
          }
          @case ('PARTNER') {
            <a routerLink="/parceiro" routerLinkActive="ativo">Área do parceiro</a>
          }
          @case ('ADMIN') {
            <a routerLink="/parceiro" routerLinkActive="ativo">Recompensas (gerir)</a>
            <a routerLink="/admin" routerLinkActive="ativo">Administração</a>
          }
        }
        @if (auth.sessao(); as s) {
          <a routerLink="/perfil" routerLinkActive="ativo" class="nav-user">
            {{ s.nome || s.email }} <small>{{ nomes[s.role] }}</small>
          </a>
          <button class="link" (click)="auth.sair()">Sair</button>
        } @else {
          <a class="nav-cta" routerLink="/entrar">Entrar <span>↗</span></a>
        }
      </nav>
    </header>
    <main><router-outlet /></main>
    <footer class="footer">
      <span>© 2026 Biciclo Floripa · Projeto acadêmico</span>
      <span>Mapa Google Maps · Clima Open-Meteo</span>
    </footer>
  `,
})
export class App {
  protected auth = inject(AuthService);
  protected menu = signal(false);
  protected nomes = NOMES_ROLE;
}
