/* BUY-01 learning header
 * File purpose: Implements the products feature behavior.
 * Learning focus: Signals, API loading states, route parameters, and custom requestAnimationFrame scrolling.
 */
import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, finalize, map, of, switchMap, tap } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../models/product.model';

@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss'
})
export class ProductDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly product = signal<Product | null>(null);
  protected readonly activeImage = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);

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
            finalize(() => this.loading.set(false))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((product) => {
        if (!product) {
          return;
        }
        this.product.set(product);
        this.activeImage.set(product.imageUrls[0] ?? null);
      });
  }
}
