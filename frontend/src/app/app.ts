/*
 * File purpose: Controls the global application shell and session actions.
 */
import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { NotificationService } from './core/services/notification.service';

@Component({
  selector: 'app-root',
  host: { '(document:keydown.escape)': 'menuOpen.set(false)' },
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly authService = inject(AuthService);
  protected readonly notifications = inject(NotificationService);
  protected readonly currentYear = new Date().getFullYear();
  protected readonly isAuthPage = signal(false);
  protected readonly menuOpen = signal(false);
  private readonly router = inject(Router);

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        const path = event.urlAfterRedirects.split(/[?#]/, 1)[0];
        this.isAuthPage.set(path === '/login' || path === '/register');
        this.menuOpen.set(false);
      });
  }

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
