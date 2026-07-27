/* BUY-01 learning header
 * File purpose: Implements the auth feature behavior.
 * Learning focus: Reactive Forms, validation, role selection, and navigation after authentication.
 */
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.authService
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe((response) => {
        this.notifications.success(`Welcome back, ${response.user.name}.`);
        const requestedUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const safeReturnUrl =
          requestedUrl?.startsWith('/') && !requestedUrl.startsWith('//')
            ? requestedUrl
            : null;
        const destination =
          safeReturnUrl ||
          (response.user.role === 'SELLER'
            ? '/seller'
            : response.user.role === 'ADMIN'
              ? '/admin'
              : '/products');
        void this.router.navigateByUrl(destination);
      });
  }
}
