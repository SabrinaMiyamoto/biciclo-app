import { Routes } from '@angular/router';
import { perfil } from './core/guards';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/inicio').then((m) => m.Inicio), title: 'Biciclo Floripa' },
  { path: 'mapa', loadComponent: () => import('./pages/mapa').then((m) => m.Mapa), title: 'Mapa · Biciclo' },
  { path: 'entrar', loadComponent: () => import('./pages/entrar').then((m) => m.Entrar), title: 'Entrar · Biciclo' },
  { path: 'recompensas', loadComponent: () => import('./pages/recompensas').then((m) => m.Recompensas), title: 'Recompensas · Biciclo' },
  { path: 'pedalar', canActivate: [perfil('CYCLIST')], loadComponent: () => import('./pages/pedalar').then((m) => m.Pedalar), title: 'Pedalar · Biciclo' },
  { path: 'alertas', canActivate: [perfil('CYCLIST')], loadComponent: () => import('./pages/alertas').then((m) => m.Alertas), title: 'Alertas · Biciclo' },
  { path: 'perfil', canActivate: [perfil()], loadComponent: () => import('./pages/perfil').then((m) => m.Perfil), title: 'Meu perfil · Biciclo' },
  { path: 'parceiro', canActivate: [perfil('PARTNER', 'ADMIN')], loadComponent: () => import('./pages/parceiro').then((m) => m.Parceiro), title: 'Parceiro · Biciclo' },
  { path: 'admin', canActivate: [perfil('ADMIN')], loadComponent: () => import('./pages/admin').then((m) => m.Admin), title: 'Administração · Biciclo' },
  { path: '**', redirectTo: '' },
];
