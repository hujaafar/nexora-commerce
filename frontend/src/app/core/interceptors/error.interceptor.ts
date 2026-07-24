import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../../models/api-error.model';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const notifications = inject(NotificationService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const body = error.error as ApiError | undefined;
      const validationMessage = body?.validationErrors
        ? Object.values(body.validationErrors)[0]
        : null;
      const message =
        validationMessage ||
        body?.message ||
        (error.status === 0
          ? 'The API is unavailable. Check that Docker Compose is running.'
          : 'The request could not be completed.');

      notifications.error(message);
      const isAuthRequest =
        request.url.includes('/auth/login') || request.url.includes('/auth/register');
      if (error.status === 401 && !isAuthRequest) {
        authService.logout();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
