import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Blocks access to the rest of the app until a member with a temporary password has changed it. */
export const forcePasswordChangeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.currentUser()?.mustChangePassword) {
    router.navigate(['/change-password']);
    return false;
  }
  return true;
};
