/* BUY-01 learning header
 * File purpose: Protects Angular routes that require admin access.
 * Learning focus: Functional route guards and role-aware navigation.
 */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.isAdmin() ? true : router.createUrlTree(['/products']);
};
