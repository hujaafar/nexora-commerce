/* File purpose: Protects cart, checkout, and wishlist routes that require a client account. */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const clientGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.isClient() ? true : router.createUrlTree(['/products']);
};
