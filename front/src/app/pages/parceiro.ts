import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService, mensagemErro } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Cupom, Recompensa, RecompensaRequest } from '../core/models';

const VAZIA: RecompensaRequest = { titulo: '', descricao: '', custoPontos: 50, quantidadeEstoque: 10, dataExpiracao: '', ativa: true };

@Component({
  selector: 'app-parceiro',
  imports: [DatePipe, FormsModule],
  template: `
    <section class="section">
      <div class="section-heading">
        <div><span class="eyebrow">ÁREA DO PARCEIRO</span><h2>Suas <em>recompensas.</em></h2></div>
        <p>Cadastre benefícios para os ciclistas e valide os cupons apresentados no balcão.</p>
      </div>
      @if (msg()) { <p class="aviso">{{ msg() }}</p> }

      <div class="duas-colunas">
        @if (auth.role() === 'PARTNER') {
          <form class="cartao" (ngSubmit)="validar()">
            <h3>Validar cupom</h3>
            <label for="codigo">Código apresentado pelo ciclista</label>
            <input id="codigo" name="codigo" [(ngModel)]="codigo" required />
            <button class="button button-primary full" [disabled]="!codigo">Validar <span>✓</span></button>
            @if (validado(); as c) {
              <p class="positivo">Cupom {{ c.codigoValidacao }} validado em {{ c.dataUtilizacao | date: 'dd/MM/yy HH:mm' }}.</p>
            }
          </form>
        }

        <form class="cartao" (ngSubmit)="salvar()">
          <h3>{{ editando() ? 'Editar recompensa' : 'Nova recompensa' }}</h3>
          <label for="titulo">Título</label>
          <input id="titulo" name="titulo" [(ngModel)]="form.titulo" required maxlength="150" />
          <label for="desc">Descrição</label>
          <textarea id="desc" name="desc" [(ngModel)]="form.descricao" rows="2" maxlength="500"></textarea>
          <div class="linha">
            <div><label for="custo">Custo (pontos)</label>
              <input id="custo" name="custo" type="number" min="0" [(ngModel)]="form.custoPontos" required /></div>
            <div><label for="estoque">Estoque</label>
              <input id="estoque" name="estoque" type="number" min="0" [(ngModel)]="form.quantidadeEstoque" required /></div>
          </div>
          <label for="validade">Válida até (vazio = sem validade)</label>
          <input id="validade" name="validade" type="date" [(ngModel)]="form.dataExpiracao" />
          <label class="check"><input type="checkbox" name="ativa" [(ngModel)]="form.ativa" /> Visível no catálogo</label>
          <button class="button button-primary full">{{ editando() ? 'Salvar alterações' : 'Cadastrar' }} <span>→</span></button>
          @if (editando()) { <button type="button" class="button button-ghost full" (click)="limpar()">Cancelar edição</button> }
        </form>
      </div>

      <h3 class="subtitulo">Minhas recompensas</h3>
      <table class="tabela">
        <thead><tr><th>Título</th><th>Pontos</th><th>Estoque</th><th>Validade</th><th>Situação</th><th></th></tr></thead>
        <tbody>
          @for (r of lista(); track r.id) {
            <tr>
              <td>{{ r.titulo }}</td>
              <td>{{ r.custoPontos }}</td>
              <td>{{ r.quantidadeEstoque }}</td>
              <td>{{ r.dataExpiracao ? (r.dataExpiracao | date: 'dd/MM/yyyy') : '—' }}</td>
              <td><span class="selo" [attr.data-status]="r.ativa ? 'APROVADO' : 'REJEITADO'">{{ r.ativa ? 'Ativa' : 'Inativa' }}</span></td>
              <td class="acoes">
                <button class="link" (click)="editar(r)">Editar</button>
                @if (r.ativa) { <button class="link" (click)="desativar(r)">Desativar</button> }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="muted">Nenhuma recompensa cadastrada.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class Parceiro implements OnInit {
  private api = inject(ApiService);
  protected auth = inject(AuthService);
  protected lista = signal<Recompensa[]>([]);
  protected editando = signal<string | null>(null);
  protected validado = signal<Cupom | null>(null);
  protected msg = signal('');
  protected form: RecompensaRequest = { ...VAZIA };
  protected codigo = '';

  ngOnInit() {
    this.carregar();
  }

  editar(r: Recompensa) {
    this.editando.set(r.id);
    this.form = {
      titulo: r.titulo, descricao: r.descricao ?? '', custoPontos: r.custoPontos,
      quantidadeEstoque: r.quantidadeEstoque, dataExpiracao: r.dataExpiracao?.slice(0, 10) ?? '', ativa: r.ativa,
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  limpar() {
    this.editando.set(null);
    this.form = { ...VAZIA };
  }

  async salvar() {
    const body = { ...this.form, dataExpiracao: this.form.dataExpiracao || null };
    const id = this.editando();
    await this.tentar(async () => {
      await firstValueFrom(id ? this.api.editarRecompensa(id, body) : this.api.criarRecompensa(body));
      this.msg.set(id ? 'Recompensa atualizada.' : 'Recompensa cadastrada.');
      this.limpar();
      await this.carregar();
    });
  }

  async desativar(r: Recompensa) {
    if (!confirm(`Desativar "${r.titulo}"? Os cupons já emitidos continuam válidos.`)) return;
    await this.tentar(async () => {
      await firstValueFrom(this.api.desativarRecompensa(r.id));
      await this.carregar();
    });
  }

  async validar() {
    this.validado.set(null);
    await this.tentar(async () => {
      this.validado.set(await firstValueFrom(this.api.validarCupom(this.codigo.trim())));
      this.codigo = '';
      this.msg.set('');
    });
  }

  private async carregar() {
    await this.tentar(async () => this.lista.set(await firstValueFrom(this.api.minhasRecompensas())));
  }

  private async tentar(f: () => Promise<void>) {
    try {
      await f();
    } catch (e) {
      this.msg.set(mensagemErro(e));
    }
  }
}
