import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Auth } from '../auth/auth';

export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const user = auth.currentUser();

  if (!user) {
    router.navigate(['/login']);
    return false;
  }

  const expectedRoles: string[] = route.data['roles'];
  if (expectedRoles && expectedRoles.length > 0) {
    if (!expectedRoles.includes(user.rol)) {
      // Redirect to home if unauthorized
      router.navigate(['/login']);
      return false;
    }
  }

  return true;
};
