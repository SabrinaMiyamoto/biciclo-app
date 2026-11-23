import { AfterViewInit, Component, ElementRef, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { NOMES_POI, Oficina, Poi, TipoPoi } from '../core/models';
import { AuthService } from '../core/auth.service';
import { DecimalPipe } from '@angular/common';
import { environment } from '../../environments/environment';
import { DistanciaPipe, DuracaoPipe } from '../shared/formatos';
import { Coord, MapaBiciclo, buscarEndereco, distanciaMetros, limpar } from '../shared/mapa';

/**
 * Rota de bicicleta pelo servidor público do FOSSGIS (o mesmo usado no openstreetmap.org),
 * gratuito e sem chave. É um serviço de demonstração: pode ficar lento ou fora do ar.
 */
const ROTEADOR = 'https://routing.openstreetmap.de/routed-bike/route/v1/driving';

@Component({
  selector: 'app-mapa',
  imports: [FormsModule, DecimalPipe, DistanciaPipe, DuracaoPipe],
  template: `
    <section class="section">
      <div class="section-heading">
        <div><span class="eyebrow">SEU PRÓXIMO CAMINHO</span><h2>Para onde vamos <em>pedalar hoje?</em></h2></div>
        <p>Mapa do Google. Os pontos de apoio vêm da API do Biciclo e os endereços do OpenStreetMap.</p>
      </div>
      <div class="route-layout">
        <div class="route-panel">
          <form (ngSubmit)="tracar()">
            <label for="origem">Ponto de partida</label>
            <input id="origem" name="origem" [(ngModel)]="origem" placeholder="Ex.: Beira-Mar Norte" required />
            <label for="destino">Destino</label>
            <input id="destino" name="destino" [(ngModel)]="destino" placeholder="Ex.: Lagoa da Conceição" required />
            <button class="button button-primary full" [disabled]="carregando()">
              {{ carregando() ? 'Buscando...' : 'Traçar rota' }} <span>→</span>
            </button>
          </form>
          @if (rota(); as r) {
            <p class="resumo-rota"><strong>{{ r.distancia | distancia }}</strong> · cerca de {{ r.duracao | duracao }}
              @if (r.linhaReta) { <br /><small>Linha reta: o roteador de bicicleta não respondeu.</small> }
            </p>
          }
          @if (msg()) { <p class="form-note">{{ msg() }}</p> }

          <label>Pontos de apoio</label>
          <div class="chips">
            @for (t of tipos; track t) {
              <button type="button" [class.ativo]="filtro().has(t)" (click)="alternar(t)">{{ nomesPoi[t] }}</button>
            }
          </div>
          <button type="button" class="button button-ghost full" (click)="carregarPois()">Buscar pontos nesta área</button>
          <p class="form-note">{{ pois().length }} ponto(s) num raio de 5 km do centro do mapa.</p>

          @if (auth.logado() && oficinasDisponivel()) {
            <button type="button" class="button button-ghost full" (click)="carregarOficinas()">Oficinas de bicicleta por perto</button>
            @if (oficinas().length) {
              <ul class="oficinas">
                @for (o of oficinas(); track o.placeId) {
                  <li>
                    <a [href]="o.googleMapsUrl" target="_blank" rel="noopener"><strong>{{ o.nome }}</strong></a>
                    <small>{{ o.endereco }}</small>
                    <small>
                      @if (o.nota) { ★ {{ o.nota | number: '1.1-1' }} ({{ o.totalAvaliacoes ?? 0 }}) · }
                      @if (o.abertoAgora === true) { Aberta agora } @else if (o.abertoAgora === false) { Fechada agora }
                    </small>
                  </li>
                }
              </ul>
              <p class="atribuicao-google">Dados: Google Maps</p>
            }
          }
        </div>
        <div class="map-wrap"><div #mapa class="mapa"></div></div>
      </div>
    </section>
  `,
})
export class Mapa implements AfterViewInit, OnDestroy {
  private api = inject(ApiService);
  protected auth = inject(AuthService);
  private el = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private mapa: MapaBiciclo | null = null;
  private camadaPois: google.maps.marker.AdvancedMarkerElement[] = [];
  private camadaOficinas: google.maps.marker.AdvancedMarkerElement[] = [];
  private camadaRota: (google.maps.marker.AdvancedMarkerElement | google.maps.Polyline)[] = [];

  protected nomesPoi = NOMES_POI;
  protected tipos = Object.keys(NOMES_POI) as TipoPoi[];
  protected filtro = signal(new Set<TipoPoi>(this.tipos));
  protected pois = signal<Poi[]>([]);
  /** Fica false se a API responder 503 (sem chave do Google Places configurada). */
  protected oficinasDisponivel = signal(true);
  /** Resultados do Google ficam só na memória da tela (termos do Google: não guardar). */
  protected oficinas = signal<Oficina[]>([]);
  protected origem = '';
  protected destino = '';
  protected carregando = signal(false);
  protected msg = signal('');
  protected rota = signal<{ distancia: number; duracao: number; linhaReta: boolean } | null>(null);

  async ngAfterViewInit() {
    this.mapa = await MapaBiciclo.criar(this.el().nativeElement);
    this.carregarPois();
  }

  ngOnDestroy() {
    this.mapa = null;
  }

  private centro(): Coord {
    return this.mapa?.centro() ?? { lat: environment.centro.latitude, lng: environment.centro.longitude };
  }

  async carregarPois() {
    const c = this.centro();
    try {
      this.pois.set(await firstValueFrom(this.api.poisProximos(c.lat, c.lng, 5000)));
      this.desenharPois();
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }

  async carregarOficinas() {
    const c = this.centro();
    try {
      const lista = await firstValueFrom(this.api.oficinasProximas(c.lat, c.lng, 3000));
      this.oficinas.set(lista);
      if (!lista.length) this.msg.set('Nenhuma oficina encontrada nesta área.');
      if (this.mapa) {
        limpar(this.camadaOficinas);
        for (const o of lista) {
          this.camadaOficinas.push(this.mapa.marcador({ lat: o.latitude, lng: o.longitude }, '🔧', '#c77b45', {
            titulo: o.nome,
            popup: `${o.nome}${o.endereco ? '\n' + o.endereco : ''}`,
          }));
        }
      }
    } catch (e) {
      if ((e as { status?: number }).status === 503) this.oficinasDisponivel.set(false);
      else this.msg.set(mensagemErro(e));
    }
  }

  alternar(t: TipoPoi) {
    const s = new Set(this.filtro());
    s.has(t) ? s.delete(t) : s.add(t);
    this.filtro.set(s);
    this.desenharPois();
  }

  private desenharPois() {
    if (!this.mapa) return;
    limpar(this.camadaPois);
    for (const p of this.pois().filter((p) => this.filtro().has(p.tipo))) {
      const cap = p.capacidade ? `\nCapacidade: ${p.capacidade}` : '';
      this.camadaPois.push(this.mapa.marcadorPoi({ lat: p.latitude, lng: p.longitude }, p.tipo, {
        titulo: p.descricao,
        popup: `${NOMES_POI[p.tipo]}\n${p.descricao}${cap}`,
      }));
    }
  }

  async tracar() {
    this.carregando.set(true);
    this.msg.set('');
    this.rota.set(null);
    try {
      const [a, b] = await Promise.all([buscarEndereco(this.origem), buscarEndereco(this.destino)]);
      if (!a || !b) {
        this.msg.set(`Não encontramos ${!a ? 'a partida' : 'o destino'} em Florianópolis.`);
        return;
      }
      const pa = { lat: a.lat, lng: a.lon }, pb = { lat: b.lat, lng: b.lon };
      let linha: Coord[];
      try {
        const r = await fetch(`${ROTEADOR}/${a.lon},${a.lat};${b.lon},${b.lat}?overview=full&geometries=geojson`);
        const dados = await r.json();
        const rota = dados.routes[0];
        linha = rota.geometry.coordinates.map(([lng, lat]: number[]) => ({ lat, lng }));
        this.rota.set({ distancia: rota.distance, duracao: rota.duration, linhaReta: false });
      } catch {
        linha = [pa, pb];
        const d = distanciaMetros(pa, pb);
        this.rota.set({ distancia: d, duracao: d / (15 / 3.6), linhaReta: true }); // 15 km/h
      }
      if (this.mapa) {
        limpar(this.camadaRota);
        this.camadaRota.push(
          this.mapa.linha(linha),
          this.mapa.marcador(pa, '○', '#2f6847', { popup: a.nome }),
          this.mapa.marcador(pb, '⌖', '#d17b43', { popup: b.nome }),
        );
        this.mapa.enquadrar(linha);
      }
      this.carregarPois();
    } catch {
      this.msg.set('Falha ao buscar endereços. Verifique a internet.');
    } finally {
      this.carregando.set(false);
    }
  }
}
