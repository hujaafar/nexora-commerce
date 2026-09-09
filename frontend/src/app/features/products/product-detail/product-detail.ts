/*
 * File purpose: Implements the products feature behavior.
 */
import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, finalize, map, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CommerceService } from '../../../core/services/commerce.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../models/product.model';

@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);
  private readonly commerce = inject(CommerceService);
  private readonly notifications = inject(NotificationService);
  protected readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly product = signal<Product | null>(null);
  protected readonly activeImage = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly quantity = signal(1);
  protected readonly adding = signal(false);
  protected readonly saved = signal(false);

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        tap(() => {
          this.loading.set(true);
          this.loadError.set(false);
          this.product.set(null);
          this.activeImage.set(null);
        }),
        switchMap((id) =>
          this.productService.get(id).pipe(
            catchError(() => {
              this.loadError.set(true);
              return of(null);
            }),
            finalize(() => this.loading.set(false)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((product) => {
        if (!product) {
          return;
        }
        this.product.set(product);
        this.activeImage.set(product.imageUrls[0] ?? null);
        if (this.authService.isClient()) {
          this.commerce
            .wishlist()
            .subscribe((wishlist) =>
              this.saved.set(wishlist.items.some((item) => item.id === product.id)),
            );
        }
      });
  }

  protected changeQuantity(value: number): void {
    const maximum = this.product()?.quantity ?? 1;
    this.quantity.set(Math.min(maximum, Math.max(1, value)));
  }

  protected addToCart(): void {
    const product = this.product();
    if (!product || !this.authService.isClient() || this.adding()) return;
    this.adding.set(true);
    this.commerce
      .putCartItem(product.id, this.quantity())
      .pipe(finalize(() => this.adding.set(false)))
      .subscribe(() => this.notifications.success(`${product.name} was added to your cart.`));
  }

  protected toggleWishlist(): void {
    const product = this.product();
    if (!product || !this.authService.isClient()) return;
    const request = this.saved()
      ? this.commerce.removeWishlist(product.id)
      : this.commerce.addWishlist(product.id);
    request.subscribe(() => {
      this.saved.update((value) => !value);
      this.notifications.show(
        this.saved() ? 'Saved for later.' : 'Removed from saved products.',
        'info',
      );
    });
  }
}
