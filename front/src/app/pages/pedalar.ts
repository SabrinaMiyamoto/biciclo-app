import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { PontoTrajeto, Trajeto } from '../core/models';
import { DistanciaPipe, DuracaoPipe } from '../shared/formatos';
import { MapaBiciclo, distanciaMetros } from '../shared/mapa';
import { environment } from '../../environments/environment';

/**
 * Grava o trajeto com o GPS do navegador (navigator.geolocation) e envia para a API,
 * que calcula distância, velocidade e pontos (10 por km, mínimo 200 m, máximo 60 km/h).
 */
@Component({
  selector: 'app-pedalar',
  imports: [DatePipe, DecimalPipe, DistanciaPipe, DuracaoPipe],
  template: `
    <section class="section">
      <div class="section-heading">
        <div><span class="eyebrow">GAMIFICAÇÃO</span><h2>Pedale e <em>ganhe pontos.</em></h2></div>
        <p>Cada km aprovado vale 10 pontos. O trajeto precisa ter pelo menos 200 m e média abaixo de 60 km/h.</p>
      </div>
      <div class="route-layout">
        <div class="route-panel">
          <div class="placar">
            <div><strong>{{ distancia() | distancia }}</strong><small>distância</small></div>
            <div><strong>{{ segundos() | duracao }}</strong><small>tempo</small></div>
            <div><strong>{{ pontos().length }}</strong><small>pontos GPS</small></div>
          </div>
          @if (!gravando()) {
            <button class="button button-primary full" (click)="iniciar()">Iniciar trajeto <span>▶</span></button>
          } @else {
            <button class="button button-laranja full" (click)="finalizar()">Finalizar e enviar <span>■</span></button>
            <button class="button button-ghost full" (click)="cancelar()">Cancelar</button>
          }
          @if (simulacao) {
            <button class="button button-ghost full" (click)="simular()" [disabled]="gravando()">
              Simular trajeto (teste em sala)
            </button>
          }
          @if (msg()) { <p class="form-note">{{ msg() }}</p> }
          @if (resultado(); as r) {
            <div class="cartao resultado" [class.rejeitado]="r.statusAnalise === 'REJEITADO'">
              <strong>{{ r.statusAnalise === 'APROVADO' ? '+' + r.pontosGanhos + ' pontos!' : 'Trajeto rejeitado' }}</strong>
              <small>{{ r.distanciaMetros | distancia }} · {{ r.velocidadeMedia | number: '1.0-1' }} km/h</small>
            </div>
          }
        </div>
        <div class="map-wrap"><div #mapa class="mapa"></div></div>
      </div>

      <h3 class="subtitulo">Meus trajetos</h3>
      <table class="tabela">
        <thead><tr><th>Data</th><th>Distância</th><th>Tempo</th><th>Média</th><th>Pontos</th><th>Situação</th></tr></thead>
        <tbody>
          @for (t of trajetos(); track t.id) {
            <tr>
              <td>{{ t.createdAt | date: 'dd/MM/yy HH:mm' }}</td>
              <td>{{ t.distanciaMetros | distancia }}</td>
              <td>{{ t.duracaoSegundos | duracao }}</td>
              <td>{{ t.velocidadeMedia | number: '1.0-1' }} km/h</td>
              <td>{{ t.pontosGanhos }}</td>
              <td><span class="selo" [attr.data-status]="t.statusAnalise">{{ t.statusAnalise }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="muted">Nenhum trajeto ainda.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class Pedalar implements OnInit, AfterViewInit, OnDestroy {
  private api = inject(ApiService);
  private el = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private mapa: MapaBiciclo | null = null;
  private linha: google.maps.Polyline | null = null;
  private watchId?: number;
  private relogio?: ReturnType<typeof setInterval>;

  protected simulacao = environment.permitirSimulacao;
  protected gravando = signal(false);
  protected pontos = signal<PontoTrajeto[]>([]);
  protected agora = signal(Date.now());
  protected msg = signal('');
  protected resultado = signal<Trajeto | null>(null);
  protected trajetos = signal<Trajeto[]>([]);

  protected distancia = computed(() => {
    const p = this.pontos();
    let d = 0;
    for (let i = 1; i < p.length; i++) {
      d += distanciaMetros({ lat: p[i - 1].latitude, lng: p[i - 1].longitude }, { lat: p[i].latitude, lng: p[i].longitude });
    }
    return d;
  });
  protected segundos = computed(() => {
    const p = this.pontos();
    if (!p.length) return 0;
    const fim = this.gravando() ? this.agora() : Date.parse(p[p.length - 1].timestamp);
    return (fim - Date.parse(p[0].timestamp)) / 1000;
  });

  ngOnInit() {
    this.carregarTrajetos();
  }

  async ngAfterViewInit() {
    this.mapa = await MapaBiciclo.criar(this.el().nativeElement, 14);
    this.linha = this.mapa?.linha([]) ?? null;
  }

  ngOnDestroy() {
    this.pararGps();
    this.mapa = null;
  }

  iniciar() {
    if (!('geolocation' in navigator)) {
      this.msg.set('Este navegador não tem GPS/localização.');
      return;
    }
    this.pontos.set([]);
    this.linha?.setPath([]);
    this.resultado.set(null);
    this.msg.set('Aguardando o sinal do GPS...');
    this.gravando.set(true);
    this.relogio = setInterval(() => this.agora.set(Date.now()), 1000);
    this.watchId = navigator.geolocation.watchPosition(
      (pos) => {
        if (pos.coords.accuracy > 50) return; // ignora leituras muito imprecisas
        // A API aceita no máximo 5000 pontos: guarda no máximo 1 ponto a cada 3 s (≈ 4 h de pedal).
        const ultimo = this.pontos().at(-1);
        if (ultimo && pos.timestamp - Date.parse(ultimo.timestamp) < 3000) return;
        this.msg.set('Gravando. Pode guardar o celular, mas deixe a tela do app aberta.');
        this.adicionar({ latitude: pos.coords.latitude, longitude: pos.coords.longitude, timestamp: new Date(pos.timestamp).toISOString() });
      },
      (err) => this.msg.set(err.code === 1 ? 'Permita o acesso à localização para gravar o trajeto.' : 'Sem sinal de GPS.'),
      { enableHighAccuracy: true, maximumAge: 0, timeout: 20000 },
    );
  }

  async finalizar() {
    this.pararGps();
    if (this.pontos().length < 2) {
      this.msg.set('Poucos pontos de GPS. Tente pedalar um pouco mais.');
      return;
    }
    await this.enviar(this.pontos());
  }

  cancelar() {
    this.pararGps();
    this.pontos.set([]);
    this.linha?.setPath([]);
    this.msg.set('');
  }

  /** Gera um passeio fictício de ~2 km pela Beira-Mar Norte a 15 km/h, terminando agora. */
  async simular() {
    const inicio: [number, number] = [-27.5828, -48.5485];
    const fim: [number, number] = [-27.5700, -48.5310];
    const n = 20, total = 8 * 60 * 1000; // 8 minutos
    const pts: PontoTrajeto[] = [];
    for (let i = 0; i <= n; i++) {
      pts.push({
        latitude: inicio[0] + ((fim[0] - inicio[0]) * i) / n,
        longitude: inicio[1] + ((fim[1] - inicio[1]) * i) / n,
        timestamp: new Date(Date.now() - total + (total * i) / n - 1000).toISOString(),
      });
    }
    this.pontos.set([]);
    this.linha?.setPath([]);
    pts.forEach((p) => this.adicionar(p));
    await this.enviar(pts);
  }

  private adicionar(p: PontoTrajeto) {
    this.pontos.update((lista) => [...lista, p]);
    const c = { lat: p.latitude, lng: p.longitude };
    this.linha?.getPath().push(new google.maps.LatLng(c));
    this.mapa?.centralizar(c);
  }

  private async enviar(pontos: PontoTrajeto[]) {
    this.msg.set('Enviando...');
    try {
      const r = await firstValueFrom(this.api.registrarTrajeto(pontos));
      this.resultado.set(r);
      this.msg.set('');
      this.carregarTrajetos();
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }

  private pararGps() {
    if (this.watchId !== undefined) navigator.geolocation.clearWatch(this.watchId);
    this.watchId = undefined;
    clearInterval(this.relogio);
    this.gravando.set(false);
  }

  private async carregarTrajetos() {
    try {
      this.trajetos.set(await firstValueFrom(this.api.meusTrajetos()));
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }
}
