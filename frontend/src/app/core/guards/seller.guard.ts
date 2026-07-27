/* BUY-01 learning header
 * File purpose: Protects Angular routes that require seller access.
 * Learning focus: Functional route guards and role-aware navigation.
 */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const sellerGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.isSeller() ? true : router.createUrlTree(['/products']);
};
