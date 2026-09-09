/* File purpose: Implements order tracking, cancellation, redo, removal, and seller fulfilment actions. */
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, map, switchMap } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CommerceService } from '../../../core/services/commerce.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MarketplaceOrder, OrderStatus } from '../../../models/commerce.model';

@Component({
  selector: 'app-order-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly commerce = inject(CommerceService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly sellerView = this.auth.isSeller;
  protected readonly order = signal<MarketplaceOrder | null>(null);
  protected readonly loading = signal(true);
  protected readonly acting = signal(false);
  protected readonly stages: OrderStatus[] = [
    'PLACED',
    'CONFIRMED',
    'PACKING',
    'SHIPPED',
    'DELIVERED',
  ];
  protected readonly nextStatus = computed<OrderStatus | null>(() => {
    const current = this.order()?.status;
    const transitions: Partial<Record<OrderStatus, OrderStatus>> = {
      PLACED: 'CONFIRMED',
      CONFIRMED: 'PACKING',
      PACKING: 'SHIPPED',
      SHIPPED: 'DELIVERED',
    };
    return current ? (transitions[current] ?? null) : null;
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        switchMap((id) => this.commerce.order(id, this.sellerView())),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((order) => this.order.set(order));
  }

  protected stageReached(stage: OrderStatus): boolean {
    const current = this.order()?.status;
    if (!current || current === 'CANCELLED') return false;
    return this.stages.indexOf(stage) <= this.stages.indexOf(current);
  }

  protected cancel(): void {
    const order = this.order();
    if (!order || this.acting()) return;
    if (!window.confirm('Cancel this order and return the reserved stock?')) return;
    this.run(this.commerce.cancelOrder(order.id), 'Order cancelled.');
  }

  protected redo(): void {
    const order = this.order();
    if (!order || this.acting()) return;
    this.run(this.commerce.redoOrder(order.id), 'A new order was created.');
  }

  protected advance(): void {
    const order = this.order();
    const status = this.nextStatus();
    if (!order || !status || this.acting()) return;
    this.run(
      this.commerce.updateOrderStatus(order.id, status),
      `Order moved to ${this.label(status)}.`,
    );
  }

  protected remove(): void {
    const order = this.order();
    if (!order || this.acting()) return;
    this.acting.set(true);
    this.commerce
      .removeOrder(order.id)
      .pipe(finalize(() => this.acting.set(false)))
      .subscribe(() => {
        this.notifications.show('Order removed from your history.', 'info');
        void this.router.navigate(['/orders']);
      });
  }

  protected label(status: OrderStatus): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  private run(request: ReturnType<CommerceService['cancelOrder']>, message: string): void {
    this.acting.set(true);
    request.pipe(finalize(() => this.acting.set(false))).subscribe((order) => {
      this.order.set(order);
      this.notifications.success(message);
    });
  }
}
