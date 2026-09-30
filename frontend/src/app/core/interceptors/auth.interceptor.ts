import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Attaches the JWT to every request aimed at our own API, and centrally
 * handles 401 (session expired -> log out) so individual components don't
 * each need their own error-handling boilerplate.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const isApiCall = req.url.startsWith(environment.apiUrl);
  const token = auth.token;

  const authReq = isApiCall && token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isApiCall) {
        if (error.status === 401 && token) {
          // Only a request that actually carried our token can have had its *session*
          // rejected. A 401 on a request with no token (e.g. a failed login attempt) is a
          // credentials error, not an expired session - logging out and showing "session
          // expired" there would be actively misleading (there was no session yet).
          auth.logout();
          toast.error('Your session has expired. Please log in again.');
        } else if (error.status === 0) {
          toast.error('Could not reach the server. Please check your connection.');
        } else if (error.status >= 500) {
          toast.error('Something went wrong on the server. Please try again.');
        } else if (error.status !== 400 && error.error?.message) {
          // 400s are typically shown inline on forms; surface everything else as a toast.
          toast.error(error.error.message);
        }
      }
      return throwError(() => error);
    })
  );
};
