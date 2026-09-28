import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Requires the logged-in user to already belong to a room (i.e. not fresh off registration). */
export const roomGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.hasRoom()) {
    return true;
  }
  router.navigate(['/room/setup']);
  return false;
};
