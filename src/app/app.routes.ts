import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'inicio', loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
  { path: 'mapa', loadComponent: () => import('./pages/mapa/mapa.component').then(m => m.MapaComponent) },
  { path: 'info', loadComponent: () => import('./pages/info/info.component').then(m => m.InfoComponent) },
  { path: 'buscar', loadComponent: () => import('./pages/buscar/buscar.component').then(m => m.BuscarComponent) },
  { path: 'pontos', loadComponent: () => import('./pages/pontos-apoio/pontos-apoio.component').then(m => m.PontosApoioComponent) },
  { path: 'pontos/:id', loadComponent: () => import('./pages/ponto-detalhe/ponto-detalhe.component').then(m => m.PontoDetalheComponent) },
  { path: '**', redirectTo: '' }
];
