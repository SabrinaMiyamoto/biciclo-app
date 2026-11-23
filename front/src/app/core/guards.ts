import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

/** Libera a rota só para quem está logado com um dos perfis informados. */
export function perfil(...roles: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.logado()) return router.createUrlTree(['/entrar'], { queryParams: { voltar: state.url } });
    if (roles.length && !roles.includes(auth.role()!)) return router.createUrlTree(['/']);
    return true;
  };
}
