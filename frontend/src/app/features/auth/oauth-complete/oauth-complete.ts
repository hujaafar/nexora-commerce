import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { OAuthPending } from '../../../models/user.model';

@Component({
  selector: 'app-oauth-complete',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './oauth-complete.html',
  styleUrl: './oauth-complete.scss',
})
export class OAuthComplete implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  protected readonly pending = signal<OAuthPending | null>(null);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });

  ngOnInit(): void {
    this.auth.pendingOAuth().subscribe({
      next: (pending) => {
        this.pending.set(pending);
        this.form.controls.name.setValue(pending.name);
        if (pending.mode === 'register') {
          this.form.controls.password.addValidators([
            Validators.minLength(8),
            Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
          ]);
          this.form.controls.password.updateValueAndValidity();
        }
        this.loading.set(false);
        if (pending.mode === 'login') this.finish();
      },
      error: () => this.loading.set(false),
    });
  }

  protected finish(): void {
    const pending = this.pending();
    if (!pending || this.submitting()) return;
    this.form.markAllAsTouched();
    if (
      pending.mode !== 'login' &&
      (this.form.controls.password.invalid ||
        (pending.mode === 'register' && this.form.controls.name.invalid))
    )
      return;
    this.submitting.set(true);
    this.error.set('');
    this.auth
      .completeOAuth({
        name: pending.mode === 'register' ? this.form.controls.name.value : undefined,
        password: pending.mode === 'login' ? undefined : this.form.controls.password.value,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          const roleDestination = { CLIENT: '/products', SELLER: '/seller', ADMIN: '/admin' };
          const requested = pending.returnUrl;
          const safe =
            requested.startsWith('/') &&
            !requested.startsWith('//') &&
            !/[\\\r\n]/.test(requested) &&
            !/^\/(?:login|register|oauth2)(?:[/?#]|$)/.test(requested);
          const destination =
            safe && requested !== '/products' ? requested : roleDestination[response.user.role];
          void this.router.navigateByUrl(destination);
        },
        error: (error: HttpErrorResponse) =>
          this.error.set(
            error.error?.message || 'Sign-in could not be completed. Please try again.',
          ),
      });
  }

  protected cancel(): void {
    this.auth.cancelOAuth().subscribe({
      next: () => void this.router.navigateByUrl('/login'),
      error: () => void this.router.navigateByUrl('/login'),
    });
  }
}
