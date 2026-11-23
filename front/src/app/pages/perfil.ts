import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { Extrato, NOMES_ROLE, Usuario } from '../core/models';

@Component({
  selector: 'app-perfil',
  imports: [DatePipe],
  template: `
    <section class="section estreita">
      @if (usuario(); as u) {
        <div class="cartao perfil">
          @if (u.fotoUrl) { <img [src]="u.fotoUrl" alt="" referrerpolicy="no-referrer" /> }
          <div>
            <h2>{{ u.nome }}</h2>
            <p class="muted">{{ u.email }} · {{ nomes[u.role] }}</p>
            @if (u.role === 'CYCLIST') { <p class="saldo">Saldo: <strong>{{ u.saldoPontos ?? 0 }} pontos</strong></p> }
          </div>
        </div>
      }
      @if (msg()) { <p class="aviso">{{ msg() }}</p> }

      <h3 class="subtitulo">Extrato de pontos</h3>
      <table class="tabela">
        <thead><tr><th>Data</th><th>Movimento</th><th>Origem</th><th>Pontos</th></tr></thead>
        <tbody>
          @for (e of extrato(); track e.id) {
            <tr>
              <td>{{ e.createdAt | date: 'dd/MM/yy HH:mm' }}</td>
              <td>{{ e.tipoMovimento === 'ENTRADA' ? 'Ganho' : 'Resgate' }}</td>
              <td>{{ e.referenciaTipo === 'TRAJETO' ? 'Trajeto' : 'Cupom' }}</td>
              <td [class.positivo]="e.tipoMovimento === 'ENTRADA'" [class.negativo]="e.tipoMovimento === 'SAIDA'">
                {{ e.tipoMovimento === 'ENTRADA' ? '+' : '−' }}{{ e.quantidade }}
              </td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="muted">Sem movimentações.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class Perfil implements OnInit {
  private api = inject(ApiService);
  protected nomes = NOMES_ROLE;
  protected usuario = signal<Usuario | null>(null);
  protected extrato = signal<Extrato[]>([]);
  protected msg = signal('');

  async ngOnInit() {
    try {
      const [u, e] = await Promise.all([firstValueFrom(this.api.meuPerfil()), firstValueFrom(this.api.meuExtrato())]);
      this.usuario.set(u);
      this.extrato.set(e);
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }
}
