/* File purpose: Implements the optional save-for-later list and move-to-cart flow. */
import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CommerceService } from '../../core/services/commerce.service';
import { NotificationService } from '../../core/services/notification.service';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-wishlist-page',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './wishlist-page.html',
  styleUrl: './wishlist-page.scss',
})
export class WishlistPage implements OnInit {
  private readonly commerce = inject(CommerceService);
  private readonly notifications = inject(NotificationService);
  protected readonly items = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly actingId = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected addToCart(product: Product): void {
    if (!product.quantity || this.actingId()) return;
    this.actingId.set(product.id);
    this.commerce
      .putCartItem(product.id, 1)
      .pipe(finalize(() => this.actingId.set(null)))
      .subscribe(() => this.notifications.success(`${product.name} was added to your cart.`));
  }

  protected remove(product: Product): void {
    this.actingId.set(product.id);
    this.commerce
      .removeWishlist(product.id)
      .pipe(finalize(() => this.actingId.set(null)))
      .subscribe((wishlist) => {
        this.items.set(wishlist.items);
        this.notifications.show('Removed from saved products.', 'info');
      });
  }

  private load(): void {
    this.commerce
      .wishlist()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((wishlist) => this.items.set(wishlist.items));
  }
}
