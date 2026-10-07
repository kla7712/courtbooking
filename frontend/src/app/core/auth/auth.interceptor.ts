import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Añade el token a las peticiones a la API y, si la API responde 401
 * con un token enviado, da la sesión por caducada.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isApiCall = request.url.startsWith('/api/');
  const token = isApiCall ? auth.currentToken() : null;

  const outgoing = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(outgoing).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token) {
        auth.expire();
      }
      return throwError(() => error);
    }),
  );
};
