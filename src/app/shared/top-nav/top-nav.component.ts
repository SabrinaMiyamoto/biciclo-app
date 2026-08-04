import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-top-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './top-nav.component.html',
  styleUrl: './top-nav.component.scss'
})
export class TopNavComponent {
  links = [
    { path: '/inicio', label: 'Início' },
    { path: '/mapa', label: 'Mapa' },
    { path: '/info', label: 'Informações' },
    { path: '/buscar', label: 'Buscar' },
    { path: '/pontos', label: 'Pontos de Apoio' }
  ];
}
