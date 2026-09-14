import { Component, inject, input, OnInit, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { OAuthProviderOption } from '../../../models/user.model';

@Component({
  selector: 'app-social-login',
  templateUrl: './social-login.html',
  styleUrl: './social-login.scss',
})
export class SocialLogin implements OnInit {
  private readonly auth = inject(AuthService);
  readonly returnUrl = input<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly providers = signal<OAuthProviderOption[]>([
    { id: 'google', name: 'Google', enabled: false, authorizationUrl: '' },
    { id: 'github', name: 'GitHub', enabled: false, authorizationUrl: '' },
  ]);

  ngOnInit(): void {
    this.auth.oauthProviders().subscribe({
      next: (providers) => {
        this.providers.set(providers);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected authorizationUrl(provider: OAuthProviderOption): string {
    // The server supplies a canonical origin, so localhost/127.0.0.1 never split the login cookie.
    return `${provider.authorizationUrl}?returnUrl=${encodeURIComponent(this.returnUrl() ?? '')}`;
  }
}
