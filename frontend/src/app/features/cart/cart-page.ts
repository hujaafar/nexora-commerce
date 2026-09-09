/* File purpose: Implements cart loading, quantity edits, removal, totals, and checkout navigation. */
import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CommerceService } from '../../core/services/commerce.service';
import { NotificationService } from '../../core/services/notification.service';
import { Cart, CartLine } from '../../models/commerce.model';

@Component({
  selector: 'app-cart-page',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './cart-page.html',
  styleUrl: './cart-page.scss',
})
export class CartPage implements OnInit {
  private readonly commerce = inject(CommerceService);
  private readonly notifications = inject(NotificationService);
  protected readonly cart = signal<Cart | null>(null);
  protected readonly loading = signal(true);
  protected readonly updatingId = signal<string | null>(null);
  protected readonly delivery = computed(() => {
    const subtotal = this.cart()?.subtotal ?? 0;
    return subtotal >= 100 || subtotal === 0 ? 0 : 4.9;
  });
  protected readonly total = computed(() => (this.cart()?.subtotal ?? 0) + this.delivery());

  ngOnInit(): void {
    this.load();
  }

  protected lineTotal(item: CartLine): number {
    return item.unitPrice * item.quantity;
  }

  protected change(item: CartLine, quantity: number): void {
    if (quantity < 1 || quantity > item.availableQuantity || this.updatingId()) return;
    this.updatingId.set(item.productId);
    this.commerce
      .putCartItem(item.productId, quantity)
      .pipe(finalize(() => this.updatingId.set(null)))
      .subscribe((cart) => this.cart.set(cart));
  }

  protected remove(item: CartLine): void {
    this.updatingId.set(item.productId);
    this.commerce
      .removeCartItem(item.productId)
      .pipe(finalize(() => this.updatingId.set(null)))
      .subscribe((cart) => {
        this.cart.set(cart);
        this.notifications.show(`${item.name} was removed from your cart.`, 'info');
      });
  }

  private load(): void {
    this.loading.set(true);
    this.commerce
      .cart()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((cart) => this.cart.set(cart));
  }
}
