/*
 * File purpose: Centralizes notification API or UI state operations.
 */
import { Injectable, signal } from '@angular/core';

export type NotificationKind = 'success' | 'error' | 'info';

export interface AppNotification {
  id: number;
  kind: NotificationKind;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 1;
  private readonly notificationSignal = signal<AppNotification | null>(null);

  readonly notification = this.notificationSignal.asReadonly();

  show(message: string, kind: NotificationKind = 'info'): void {
    const notification = { id: this.nextId++, kind, message };
    this.notificationSignal.set(notification);
    window.setTimeout(() => {
      if (this.notificationSignal()?.id === notification.id) {
        this.dismiss();
      }
    }, 5000);
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  dismiss(): void {
    this.notificationSignal.set(null);
  }
}
