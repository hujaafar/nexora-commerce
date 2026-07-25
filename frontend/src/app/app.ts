import { Component, inject } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { NotificationService } from './core/services/notification.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly authService = inject(AuthService);
  protected readonly notifications = inject(NotificationService);
  protected readonly currentYear = new Date().getFullYear();
  private readonly router = inject(Router);

  protected initials(): string {
    const name = this.authService.currentUser()?.name ?? '';
    return (
      name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0])
        .join('')
        .toUpperCase() || 'U'
    );
  }

  protected logout(): void {
    this.authService.logout();
    this.notifications.show('You have been signed out.', 'info');
    void this.router.navigate(['/products']);
  }
}
