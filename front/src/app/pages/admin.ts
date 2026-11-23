import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, inject, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Alerta, NOMES_POI, NOMES_ROLE, Poi, PoiRequest, Role, StatusAlerta, TipoPoi, Usuario } from '../core/models';
import { MapaBiciclo, limpar } from '../shared/mapa';

type Aba = 'alertas' | 'pois' | 'usuarios';

@Component({
  selector: 'app-admin',
  imports: [DatePipe, FormsModule],
  template: `
    <section class="section">
      <div class="section-heading">
        <div><span class="eyebrow">ADMINISTRAÇÃO</span><h2>Painel do <em>Biciclo.</em></h2></div>
      </div>
      <div class="abas">
        <button [class.ativo]="aba() === 'alertas'" (click)="trocar('alertas')">Alertas</button>
        <button [class.ativo]="aba() === 'pois'" (click)="trocar('pois')">Pontos de apoio</button>
        <button [class.ativo]="aba() === 'usuarios'" (click)="trocar('usuarios')">Usuários</button>
      </div>
      @if (msg()) { <p class="aviso">{{ msg() }}</p> }

      <!-- ALERTAS -->
      <div [hidden]="aba() !== 'alertas'">
        <div class="chips">
          <button [class.ativo]="!filtroAlerta()" (click)="filtrarAlertas(undefined)">Todos</button>
          @for (s of statusAlerta; track s) {
            <button [class.ativo]="filtroAlerta() === s" (click)="filtrarAlertas(s)">{{ s }}</button>
          }
        </div>
        <table class="tabela">
          <thead><tr><th>Data</th><th>Tipo</th><th>Descrição</th><th>Local</th><th>Situação</th></tr></thead>
          <tbody>
            @for (a of alertas(); track a.id) {
              <tr>
                <td>{{ a.createdAt | date: 'dd/MM/yy HH:mm' }}</td>
                <td>{{ a.tipo }}</td>
                <td>{{ a.descricao }} @if (a.fotoUrl) { <a [href]="a.fotoUrl" target="_blank" rel="noopener">foto</a> }</td>
                <td><a [href]="osm(a.latitude, a.longitude)" target="_blank" rel="noopener">ver no mapa</a></td>
                <td>
                  <select [ngModel]="a.status" (ngModelChange)="mudarStatus(a, $event)">
                    @for (s of statusAlerta; track s) { <option [value]="s">{{ s }}</option> }
                  </select>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="muted">Nenhum alerta.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- POIS -->
      <div [hidden]="aba() !== 'pois'">
        <div class="route-layout">
          <form class="route-panel" (ngSubmit)="salvarPoi()">
            <h3>{{ poiEditando() ? 'Editar ponto' : 'Novo ponto' }}</h3>
            <label for="ptipo">Tipo</label>
            <select id="ptipo" name="ptipo" [(ngModel)]="poi.tipo">
              @for (t of tiposPoi; track t) { <option [value]="t">{{ nomesPoi[t] }}</option> }
            </select>
            <label for="pdesc">Descrição</label>
            <input id="pdesc" name="pdesc" [(ngModel)]="poi.descricao" required maxlength="200" />
            <label for="pcap">Capacidade (bicicletários)</label>
            <input id="pcap" name="pcap" type="number" min="0" [(ngModel)]="poi.capacidade" />
            <p class="form-note">Local: {{ poi.latitude.toFixed(5) }}, {{ poi.longitude.toFixed(5) }} (clique no mapa para mudar)</p>
            <button class="button button-primary full">{{ poiEditando() ? 'Salvar' : 'Cadastrar' }} <span>→</span></button>
            @if (poiEditando()) {
              <button type="button" class="button button-ghost full" (click)="desativarPoi()">Desativar ponto</button>
              <button type="button" class="button button-ghost full" (click)="novoPoi()">Cancelar</button>
            }
            <hr />
            <button type="button" class="button button-ghost full" (click)="importarOsm()" [disabled]="importando()">
              {{ importando() ? 'Importando...' : 'Importar do OpenStreetMap' }}
            </button>
            <p class="form-note">Busca bicicletários, oficinas, pontos de apoio e terminais na região de Florianópolis.</p>
          </form>
          <div class="map-wrap"><div #mapa class="mapa"></div></div>
        </div>
      </div>

      <!-- USUÁRIOS -->
      <div [hidden]="aba() !== 'usuarios'">
        <table class="tabela">
          <thead><tr><th>Nome</th><th>E-mail</th><th>Pontos</th><th>Perfil</th></tr></thead>
          <tbody>
            @for (u of usuarios(); track u.id) {
              <tr>
                <td>{{ u.nome }}</td>
                <td>{{ u.email }}</td>
                <td>{{ u.saldoPontos ?? 0 }}</td>
                <td>
                  <select [ngModel]="u.role" (ngModelChange)="mudarRole(u, $event)" [disabled]="u.id === auth.sessao()?.id">
                    @for (r of roles; track r) { <option [value]="r">{{ nomesRole[r] }}</option> }
                  </select>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class Admin implements OnInit, AfterViewInit, OnDestroy {
  private api = inject(ApiService);
  protected auth = inject(AuthService);
  private el = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private mapa: MapaBiciclo | null = null;
  private camada: google.maps.marker.AdvancedMarkerElement[] = [];
  private marcadorNovo: google.maps.marker.AdvancedMarkerElement | null = null;
  private mapaPronto?: Promise<void>;

  protected aba = signal<Aba>('alertas');
  protected msg = signal('');
  protected statusAlerta: StatusAlerta[] = ['ABERTO', 'EM_ANALISE', 'RESOLVIDO'];
  protected filtroAlerta = signal<StatusAlerta | undefined>(undefined);
  protected alertas = signal<Alerta[]>([]);
  protected tiposPoi = Object.keys(NOMES_POI) as TipoPoi[];
  protected nomesPoi = NOMES_POI;
  protected poi: PoiRequest = this.poiVazio();
  protected poiEditando = signal<string | null>(null);
  protected importando = signal(false);
  protected roles = Object.keys(NOMES_ROLE) as Role[];
  protected nomesRole = NOMES_ROLE;
  protected usuarios = signal<Usuario[]>([]);

  ngOnInit() {
    this.carregarAlertas();
  }

  ngAfterViewInit() {
    this.mapaPronto = MapaBiciclo.criar(this.el().nativeElement).then((m) => {
      this.mapa = m;
      m?.aoClicar((p) => {
        this.poi.latitude = p.lat;
        this.poi.longitude = p.lng;
        this.marcadorNovo = m.mover(this.marcadorNovo, p, '＋', '#c77b45');
      });
      m?.aoMover(() => this.aba() === 'pois' && this.carregarPois());
    });
  }

  ngOnDestroy() {
    this.mapa = null;
  }

  trocar(a: Aba) {
    this.aba.set(a);
    this.msg.set('');
    if (a === 'pois') {
      this.mapaPronto?.then(() => this.carregarPois());
    }
    if (a === 'usuarios') this.tentar(async () => this.usuarios.set(await firstValueFrom(this.api.adminUsuarios())));
  }

  osm(lat: number, lon: number) {
    return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=18/${lat}/${lon}`;
  }

  filtrarAlertas(s?: StatusAlerta) {
    this.filtroAlerta.set(s);
    this.carregarAlertas();
  }

  mudarStatus(a: Alerta, status: StatusAlerta) {
    this.tentar(async () => {
      await firstValueFrom(this.api.adminStatusAlerta(a.id, status));
      await this.carregarAlertas();
    });
  }

  mudarRole(u: Usuario, role: Role) {
    if (!confirm(`Mudar ${u.nome || u.email} para ${NOMES_ROLE[role]}?`)) {
      this.usuarios.update((l) => [...l]); // devolve o select ao valor anterior
      return;
    }
    this.tentar(async () => {
      const atualizado = await firstValueFrom(this.api.adminAlterarRole(u.id, role));
      this.usuarios.update((l) => l.map((x) => (x.id === u.id ? atualizado : x)));
    });
  }

  novoPoi() {
    this.poiEditando.set(null);
    this.poi = this.poiVazio();
    if (this.marcadorNovo) this.marcadorNovo.map = null;
  }

  salvarPoi() {
    const id = this.poiEditando();
    this.tentar(async () => {
      await firstValueFrom(id ? this.api.adminEditarPoi(id, this.poi) : this.api.adminCriarPoi(this.poi));
      this.msg.set(id ? 'Ponto atualizado.' : 'Ponto cadastrado.');
      this.novoPoi();
      await this.carregarPois();
    });
  }

  desativarPoi() {
    const id = this.poiEditando();
    if (!id || !confirm('Desativar este ponto? Ele some da busca pública.')) return;
    this.tentar(async () => {
      await firstValueFrom(this.api.adminDesativarPoi(id));
      this.novoPoi();
      await this.carregarPois();
    });
  }

  importarOsm() {
    this.importando.set(true);
    this.tentar(async () => {
      const r = await firstValueFrom(this.api.adminImportarOsm());
      this.msg.set(`OpenStreetMap: ${r.recebidos} recebidos, ${r.criados} novos, ${r.atualizados} atualizados, ${r.ignorados} ignorados.`);
      await this.carregarPois();
    }).finally(() => this.importando.set(false));
  }

  private editarPoi(p: Poi) {
    this.poiEditando.set(p.id);
    this.poi = { tipo: p.tipo, descricao: p.descricao, latitude: p.latitude, longitude: p.longitude, capacidade: p.capacidade ?? null, ativo: true };
    if (this.mapa) this.marcadorNovo = this.mapa.mover(this.marcadorNovo, { lat: p.latitude, lng: p.longitude }, '＋', '#c77b45');
  }

  private async carregarPois() {
    const mapa = this.mapa;
    if (!mapa) return;
    const c = mapa.centro();
    await this.tentar(async () => {
      const pois = await firstValueFrom(this.api.poisProximos(c.lat, c.lng, 5000));
      limpar(this.camada);
      for (const p of pois) {
        this.camada.push(mapa.marcadorPoi({ lat: p.latitude, lng: p.longitude }, p.tipo, {
          titulo: p.descricao,
          aoClicar: () => this.editarPoi(p),
        }));
      }
    });
  }

  private async carregarAlertas() {
    await this.tentar(async () => this.alertas.set(await firstValueFrom(this.api.adminAlertas(this.filtroAlerta()))));
  }

  private poiVazio(): PoiRequest {
    return { tipo: 'BICICLETARIO', descricao: '', latitude: -27.5954, longitude: -48.548, capacidade: null };
  }

  private async tentar(f: () => Promise<void>) {
    try {
      await f();
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }
}
