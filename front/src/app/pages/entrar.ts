import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, DadosParceiro } from '../core/auth.service';
import { mensagemErro } from '../core/api.service';
import { Role } from '../core/models';
import { buscarEndereco } from '../shared/mapa';

@Component({
  selector: 'app-entrar',
  imports: [FormsModule],
  template: `
    <section class="section estreita">
      <div class="abas">
        <button [class.ativo]="modo() === 'entrar'" (click)="modo.set('entrar')">Entrar</button>
        <button [class.ativo]="modo() === 'cadastrar'" (click)="modo.set('cadastrar')">Criar conta</button>
      </div>

      @if (auth.firebaseNaoConfigurado) {
        <p class="aviso">Falta configurar o Firebase: preencha <code>apiKey</code> e <code>appId</code> em
          <code>src/environments/environment.ts</code> (veja o README).</p>
      }
      @if (expirou()) { <p class="aviso">Sua sessão expirou. Entre novamente.</p> }

      <form class="cartao" (ngSubmit)="enviar()">
        @if (modo() === 'cadastrar') {
          <label>Quero me cadastrar como</label>
          <div class="chips">
            <button type="button" [class.ativo]="role === 'CYCLIST'" (click)="role = 'CYCLIST'">🚴 Ciclista</button>
            <button type="button" [class.ativo]="role === 'PARTNER'" (click)="role = 'PARTNER'">🏪 Parceiro (comércio)</button>
          </div>
          <label for="nome">Nome</label>
          <input id="nome" name="nome" [(ngModel)]="nome" required maxlength="150" />
          @if (role === 'PARTNER') {
            <label for="fantasia">Nome fantasia</label>
            <input id="fantasia" name="fantasia" [(ngModel)]="nomeFantasia" required />
            <label for="cnpj">CNPJ</label>
            <input id="cnpj" name="cnpj" [(ngModel)]="cnpj" placeholder="12.345.678/0001-90" required />
            <label for="endereco">Endereço do estabelecimento</label>
            <input id="endereco" name="endereco" [(ngModel)]="endereco" placeholder="Rua, número, bairro" required />
          }
        }
        <label for="email">E-mail</label>
        <input id="email" name="email" type="email" [(ngModel)]="email" required autocomplete="email" />
        <label for="senha">Senha</label>
        <input id="senha" name="senha" type="password" [(ngModel)]="senha" required minlength="6"
               [autocomplete]="modo() === 'entrar' ? 'current-password' : 'new-password'" />
        <button class="button button-primary full" [disabled]="carregando()">
          {{ modo() === 'entrar' ? 'Entrar' : 'Criar conta' }} <span>→</span>
        </button>
        <button type="button" class="button button-ghost full" (click)="google()" [disabled]="carregando()">
          Continuar com Google
        </button>
        @if (erro()) { <p class="erro">{{ erro() }}</p> }
      </form>
    </section>
  `,
})
export class Entrar {
  protected auth = inject(AuthService);
  private router = inject(Router);

  /** Query params ?voltar=/rota e ?expirou=1 (ligados por withComponentInputBinding). */
  voltar = input<string>();
  expirou = input<string>();

  protected modo = signal<'entrar' | 'cadastrar'>('entrar');
  protected carregando = signal(false);
  protected erro = signal('');
  protected role: Role = 'CYCLIST';
  protected nome = '';
  protected email = '';
  protected senha = '';
  protected nomeFantasia = '';
  protected cnpj = '';
  protected endereco = '';

  async enviar() {
    await this.executar(async () => {
      if (this.modo() === 'entrar') return this.auth.entrarComEmail(this.email, this.senha);
      return this.auth.cadastrarComEmail(this.nome, this.email, this.senha, this.role, await this.dadosParceiro());
    });
  }

  async google() {
    await this.executar(async () =>
      this.modo() === 'entrar' ? this.auth.entrarComGoogle() : this.auth.cadastrarComGoogle(this.role, await this.dadosParceiro()),
    );
  }

  private async dadosParceiro(): Promise<DadosParceiro | undefined> {
    if (this.modo() !== 'cadastrar' || this.role !== 'PARTNER') return undefined;
    const local = await buscarEndereco(this.endereco);
    if (!local) throw new Error('Não encontramos esse endereço em Florianópolis.');
    return { nomeFantasia: this.nomeFantasia, cnpj: this.cnpj, latitude: local.lat, longitude: local.lon };
  }

  private async executar(acao: () => Promise<{ role: Role }>) {
    this.carregando.set(true);
    this.erro.set('');
    try {
      const r = await acao();
      const destino = this.voltar() ?? (r.role === 'PARTNER' ? '/parceiro' : r.role === 'ADMIN' ? '/admin' : '/pedalar');
      this.router.navigateByUrl(destino);
    } catch (e) {
      this.erro.set(mensagemErro(e));
    } finally {
      this.carregando.set(false);
    }
  }
}
