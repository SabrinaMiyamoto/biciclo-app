import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { environment } from '../../environments/environment';

interface Clima { temp: number; vento: number; descricao: string; icone: string }

// Códigos WMO usados pelo Open-Meteo (mesma tabela do protótipo).
const CLIMA: [number[], string, string][] = [
  [[0], 'Céu limpo', '☀'],
  [[1, 2], 'Poucas nuvens', '⛅'],
  [[3], 'Nublado', '☁'],
  [[45, 48], 'Neblina', '🌫'],
  [[51, 53, 55, 56, 57], 'Garoa', '🌦'],
  [[61, 63, 65, 66, 67, 80, 81, 82], 'Chuva', '🌧'],
  [[95, 96, 99], 'Tempestade', '⛈'],
];

@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  template: `
    <section class="hero">
      <div class="hero-content">
        <span class="eyebrow"><span class="live-dot"></span> MOBILIDADE URBANA EM FLORIANÓPOLIS</span>
        <h1>Floripa fica melhor<br />quando a gente <em>pedala.</em></h1>
        <p>Encontre pontos de apoio, registre seus trajetos, ganhe pontos e troque por recompensas de parceiros.</p>
        <div class="hero-actions">
          <a class="button button-primary" routerLink="/mapa">Abrir o mapa <span>→</span></a>
          @if (!auth.logado()) {
            <a class="button button-ghost" routerLink="/entrar">Criar conta</a>
          } @else if (auth.role() === 'CYCLIST') {
            <a class="button button-ghost" routerLink="/pedalar">Começar a pedalar</a>
          }
        </div>
      </div>
      <div class="weather-card">
        <div class="weather-head"><span>☼ CLIMA EM FLORIANÓPOLIS</span><span class="weather-status">AGORA</span></div>
        @if (clima(); as c) {
          <div class="weather-main">
            <div><strong>{{ c.temp }}°</strong><span>{{ c.descricao }}</span></div>
            <span class="weather-icon">{{ c.icone }}</span>
          </div>
          <div class="weather-foot"><span>Vento <b>{{ c.vento }} km/h</b></span><span>Open-Meteo</span></div>
        } @else {
          <p class="muted">{{ erroClima() ? 'Não foi possível carregar o clima.' : 'Carregando clima...' }}</p>
        }
      </div>
    </section>

    <section class="quick-stats">
      <a routerLink="/mapa"><span class="stat-icon">⌖</span><strong>Mapa</strong><small>Rotas e pontos de apoio</small></a>
      <a routerLink="/pedalar"><span class="stat-icon">🚴</span><strong>Pedalar</strong><small>10 pontos por km</small></a>
      <a routerLink="/recompensas"><span class="stat-icon">🎁</span><strong>Recompensas</strong><small>Troque seus pontos</small></a>
      <a routerLink="/alertas"><span class="stat-icon">⚠</span><strong>Alertas</strong><small>Avise sobre a via</small></a>
    </section>

    <section class="info-band">
      <div><span class="eyebrow">ANTES DE SAIR</span><h2>Informação também faz parte do <em>percurso.</em></h2></div>
      <div class="safety-list">
        <div><span>01</span><p><strong>Visibilidade</strong><small>Use iluminação e itens refletivos.</small></p></div>
        <div><span>02</span><p><strong>Equipamentos</strong><small>Verifique freios, pneus e capacete.</small></p></div>
        <div><span>03</span><p><strong>Atenção</strong><small>Respeite a sinalização e as regras de trânsito.</small></p></div>
      </div>
    </section>
  `,
})
export class Inicio implements OnInit {
  protected auth = inject(AuthService);
  protected clima = signal<Clima | null>(null);
  protected erroClima = signal(false);

  async ngOnInit() {
    const { latitude, longitude } = environment.centro;
    try {
      const r = await fetch(
        `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,weather_code,wind_speed_10m&timezone=America%2FSao_Paulo`,
      );
      const { current } = await r.json();
      const [, descricao, icone] = CLIMA.find(([cods]) => cods.includes(current.weather_code)) ?? [[], 'Tempo variável', '🌤'];
      this.clima.set({ temp: Math.round(current.temperature_2m), vento: Math.round(current.wind_speed_10m), descricao, icone });
    } catch {
      this.erroClima.set(true);
    }
  }
}
