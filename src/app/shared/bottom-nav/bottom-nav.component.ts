import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-bottom-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './bottom-nav.component.html',
  styleUrl: './bottom-nav.component.scss'
})
export class BottomNavComponent {
  items: NavItem[] = [
    { path: '/inicio', label: 'Início', icon: 'home' },
    { path: '/mapa', label: 'Mapa', icon: 'map' },
    { path: '/buscar', label: 'Buscar', icon: 'search' },
    { path: '/pontos', label: 'Pontos', icon: 'pin' },
    { path: '/info', label: 'Info', icon: 'info' }
  ];
}
