import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';
  senha = '';
  erro = '';

  constructor(private router: Router) {}

  entrar() {
    if (!this.email || !this.senha) {
      this.erro = 'Preencha e-mail e senha para continuar.';
      return;
    }
    this.erro = '';
    // Login fictício (sem backend) — apenas navega para o app.
    this.router.navigate(['/inicio']);
  }

  entrarComoVisitante() {
    this.router.navigate(['/inicio']);
  }
}
