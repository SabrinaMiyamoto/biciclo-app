import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Cupom, Recompensa } from '../core/models';

@Component({
  selector: 'app-recompensas',
  imports: [DatePipe, RouterLink],
  template: `
    <section class="section">
      <div class="section-heading">
        <div><span class="eyebrow">PARCEIROS BICICLO</span><h2>Troque seus pontos por <em>recompensas.</em></h2></div>
        @if (saldo() !== null) { <p class="saldo">Seu saldo: <strong>{{ saldo() }} pontos</strong></p> }
      </div>
      @if (msg()) { <p class="aviso">{{ msg() }}</p> }
      <div class="grade">
        @for (r of recompensas(); track r.id) {
          <article class="support-card">
            <span class="card-tag">{{ r.custoPontos }} PONTOS</span>
            <h3>{{ r.titulo }}</h3>
            <p>{{ r.descricao }}</p>
            <small class="muted">{{ r.quantidadeEstoque }} disponível(is)
              @if (r.dataExpiracao) { · até {{ r.dataExpiracao | date: 'dd/MM/yyyy' }} }</small>
            @if (auth.role() === 'CYCLIST') {
              <button class="button button-primary full" (click)="resgatar(r)"
                      [disabled]="resgatando() === r.id || (saldo() ?? 0) < r.custoPontos">
                {{ (saldo() ?? 0) < r.custoPontos ? 'Pontos insuficientes' : 'Resgatar' }}
              </button>
            } @else if (!auth.logado()) {
              <a class="button button-ghost full" routerLink="/entrar">Entre para resgatar</a>
            }
          </article>
        } @empty {
          <p class="muted">Nenhuma recompensa disponível no momento.</p>
        }
      </div>

      @if (auth.role() === 'CYCLIST') {
        <h3 class="subtitulo">Meus cupons</h3>
        <p class="muted">Mostre o código ao parceiro para ele validar.</p>
        <table class="tabela">
          <thead><tr><th>Recompensa</th><th>Código</th><th>Resgate</th><th>Situação</th></tr></thead>
          <tbody>
            @for (c of cupons(); track c.id) {
              <tr>
                <td>{{ titulo(c.recompensaId) }}</td>
                <td><code class="codigo">{{ c.codigoValidacao }}</code></td>
                <td>{{ c.dataResgate | date: 'dd/MM/yy HH:mm' }}</td>
                <td><span class="selo" [attr.data-status]="c.status">{{ c.status }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="muted">Você ainda não resgatou cupons.</td></tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class Recompensas implements OnInit {
  private api = inject(ApiService);
  protected auth = inject(AuthService);
  protected recompensas = signal<Recompensa[]>([]);
  protected cupons = signal<Cupom[]>([]);
  protected saldo = signal<number | null>(null);
  protected resgatando = signal<string | null>(null);
  protected msg = signal('');

  async ngOnInit() {
    await this.carregar();
  }

  /** O catálogo só lista recompensas ativas; cupons antigos podem apontar para outras. */
  titulo(id: string) {
    return this.recompensas().find((r) => r.id === id)?.titulo ?? 'Recompensa';
  }

  async resgatar(r: Recompensa) {
    if (!confirm(`Trocar ${r.custoPontos} pontos por "${r.titulo}"?`)) return;
    this.resgatando.set(r.id);
    try {
      const c = await firstValueFrom(this.api.resgatar(r.id));
      this.msg.set(`Cupom gerado! Código: ${c.codigoValidacao}`);
      await this.carregar();
    } catch (e) {
      this.msg.set(mensagemErro(e));
    } finally {
      this.resgatando.set(null);
    }
  }

  private async carregar() {
    try {
      this.recompensas.set(await firstValueFrom(this.api.recompensas()));
      if (this.auth.role() === 'CYCLIST') {
        const [perfil, cupons] = await Promise.all([firstValueFrom(this.api.meuPerfil()), firstValueFrom(this.api.meusCupons())]);
        this.saldo.set(perfil.saldoPontos ?? 0);
        this.cupons.set(cupons);
      }
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }
}
