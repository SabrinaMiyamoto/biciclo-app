import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, inject, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { Alerta } from '../core/models';
import { Coord, MapaBiciclo } from '../shared/mapa';

/** Tipos sugeridos: a API aceita qualquer texto de até 50 caracteres. */
export const TIPOS_ALERTA = ['BURACO', 'ILUMINACAO', 'SINALIZACAO', 'OBSTRUCAO', 'ALAGAMENTO', 'OUTRO'];

@Component({
  selector: 'app-alertas',
  imports: [DatePipe, FormsModule],
  template: `
    <section class="section">
      <div class="section-heading">
        <div><span class="eyebrow">CONDIÇÕES DA VIA</span><h2>Viu um problema? <em>Avise a gente.</em></h2></div>
        <p>Clique no mapa para marcar o local exato. A equipe do Biciclo analisa cada alerta.</p>
      </div>
      <div class="route-layout">
        <form class="route-panel" (ngSubmit)="enviar()">
          <label for="tipo">Tipo</label>
          <select id="tipo" name="tipo" [(ngModel)]="tipo">
            @for (t of tipos; track t) { <option [value]="t">{{ t }}</option> }
          </select>
          <label for="descricao">Descrição</label>
          <textarea id="descricao" name="descricao" [(ngModel)]="descricao" rows="3" maxlength="500" required
                    placeholder="Ex.: buraco grande na ciclovia perto do trapiche"></textarea>
          <label for="foto">Link de uma foto (opcional)</label>
          <input id="foto" name="foto" type="url" [(ngModel)]="fotoUrl" placeholder="https://..." />
          <p class="form-note">
            @if (local(); as l) { Local: {{ l.lat.toFixed(5) }}, {{ l.lng.toFixed(5) }} } @else { Clique no mapa para escolher o local. }
          </p>
          <button type="button" class="button button-ghost full" (click)="minhaLocalizacao()">Usar minha localização</button>
          <button class="button button-primary full" [disabled]="!local() || !descricao">Enviar alerta <span>→</span></button>
          @if (msg()) { <p class="form-note">{{ msg() }}</p> }
        </form>
        <div class="map-wrap"><div #mapa class="mapa"></div></div>
      </div>

      <h3 class="subtitulo">Meus alertas</h3>
      <table class="tabela">
        <thead><tr><th>Data</th><th>Tipo</th><th>Descrição</th><th>Situação</th></tr></thead>
        <tbody>
          @for (a of alertas(); track a.id) {
            <tr>
              <td>{{ a.createdAt | date: 'dd/MM/yy HH:mm' }}</td>
              <td>{{ a.tipo }}</td>
              <td>{{ a.descricao }}</td>
              <td><span class="selo" [attr.data-status]="a.status">{{ a.status }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="muted">Você ainda não enviou alertas.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class Alertas implements OnInit, AfterViewInit, OnDestroy {
  private api = inject(ApiService);
  private el = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private mapa: MapaBiciclo | null = null;
  private marcador: google.maps.marker.AdvancedMarkerElement | null = null;

  protected tipos = TIPOS_ALERTA;
  protected tipo = 'BURACO';
  protected descricao = '';
  protected fotoUrl = '';
  protected local = signal<Coord | null>(null);
  protected alertas = signal<Alerta[]>([]);
  protected msg = signal('');

  ngOnInit() {
    this.carregar();
  }

  async ngAfterViewInit() {
    this.mapa = await MapaBiciclo.criar(this.el().nativeElement, 14);
    this.mapa?.aoClicar((p) => this.marcar(p));
  }

  ngOnDestroy() {
    this.mapa = null;
  }

  minhaLocalizacao() {
    navigator.geolocation?.getCurrentPosition(
      (p) => this.marcar({ lat: p.coords.latitude, lng: p.coords.longitude }, true),
      () => this.msg.set('Não foi possível obter sua localização.'),
    );
  }

  async enviar() {
    const l = this.local();
    if (!l) return;
    try {
      await firstValueFrom(this.api.criarAlerta({
        tipo: this.tipo, descricao: this.descricao, latitude: l.lat, longitude: l.lng, fotoUrl: this.fotoUrl || undefined,
      }));
      this.msg.set('Alerta enviado. Obrigado!');
      this.descricao = '';
      this.fotoUrl = '';
      this.carregar();
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }

  private marcar(p: Coord, centralizar = false) {
    this.local.set(p);
    if (!this.mapa) return;
    this.marcador = this.mapa.mover(this.marcador, p, '⚠', '#c77b45');
    if (centralizar) this.mapa.centralizar(p, 17);
  }

  private async carregar() {
    try {
      this.alertas.set(await firstValueFrom(this.api.meusAlertas()));
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }
}
