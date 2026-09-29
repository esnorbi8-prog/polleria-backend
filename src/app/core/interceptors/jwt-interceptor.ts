import { HttpInterceptorFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Auth } from '../auth/auth';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(Auth);
  const router = inject(Router);
  const token = authService.getToken();

  let modifiedReq = req;
  if (token) {
    modifiedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(modifiedReq).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        const newToken = event.headers.get('X-New-Token');
        if (newToken) {
          authService.setToken(newToken);
        }
      }
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
        alert('Tu sesión ha expirado por inactividad. Por favor, vuelve a ingresar.');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
