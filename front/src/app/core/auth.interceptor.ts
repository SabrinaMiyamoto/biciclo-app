import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/** Coloca "Authorization: Bearer <jwt>" nas chamadas à API e trata o 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const paraApi = req.url.startsWith(environment.apiUrl);
  const token = auth.token;
  const r = paraApi && token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(r).pipe(
    catchError((e) => {
      if (paraApi && e.status === 401 && token) auth.sessaoExpirada();
      return throwError(() => e);
    }),
  );
};
