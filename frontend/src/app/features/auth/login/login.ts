/*
 * File purpose: Implements the auth feature behavior.
 */
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SocialLogin } from '../social-login/social-login';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, SocialLogin],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
  protected readonly oauthError = signal('');

  ngOnInit(): void {
    const provider = this.route.snapshot.queryParamMap.get('oauthError');
    if (provider) {
      this.oauthError.set(
        `${provider === 'github' ? 'GitHub' : 'Google'} sign-in was cancelled or could not be completed. Please try again.`,
      );
    }
  }
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
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
          requestedUrl?.startsWith('/') && !requestedUrl.startsWith('//') ? requestedUrl : null;
        const roleDestinations = { SELLER: '/seller', ADMIN: '/admin', CLIENT: '/products' };
        const destination = safeReturnUrl || roleDestinations[response.user.role];
        void this.router.navigateByUrl(destination);
      });
  }
}
