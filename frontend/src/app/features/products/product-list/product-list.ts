/*
 * File purpose: Implements the storefront, slow section scrolling, product search facets, pagination, cart, and wishlist actions.
 */
import { CurrencyPipe } from '@angular/common';
import {
  Component,
  computed,
  DestroyRef,
  HostListener,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CommerceService } from '../../../core/services/commerce.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../models/product.model';
import { StorefrontMotion } from './storefront-motion';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink, StorefrontMotion],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  private readonly commerce = inject(CommerceService);
  private readonly notifications = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly authService = inject(AuthService);
  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  private requestVersion = 0;
  protected readonly actionId = signal<string | null>(null);
  protected readonly savedIds = signal<Set<string>>(new Set());
  protected readonly filters = this.formBuilder.group({
    q: [''],
    category: [''],
    minPrice: [null as number | null],
    maxPrice: [null as number | null],
    sort: this.formBuilder.nonNullable.control<'newest' | 'price-asc' | 'price-desc' | 'name'>(
      'newest',
    ),
  });
  protected readonly totalUnits = computed(() =>
    this.products().reduce((total, item) => total + item.quantity, 0),
  );
  protected readonly sellers = computed(
    () => new Set(this.products().map((item) => item.sellerId)).size,
  );
  private scrollFrame: number | null = null;
  private scrolling = false;
  private previousScrollBehavior = '';

  ngOnInit(): void {
    this.filters.valueChanges
      .pipe(debounceTime(320), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(0);
        this.load();
      });
    this.load();
    if (this.authService.isClient()) {
      this.commerce
        .wishlist()
        .subscribe((wishlist) => this.savedIds.set(new Set(wishlist.items.map((item) => item.id))));
    }
  }

  ngOnDestroy(): void {
    this.cancelAnimatedScroll();
  }

  protected addToCart(product: Product): void {
    if (!this.authService.isAuthenticated()) {
      this.notifications.show('Sign in as a client to start a cart.', 'info');
      return;
    }
    if (!this.authService.isClient() || !product.quantity || this.actionId()) return;
    this.actionId.set(product.id);
    this.commerce
      .putCartItem(product.id, 1)
      .pipe(finalize(() => this.actionId.set(null)))
      .subscribe(() => this.notifications.success(`${product.name} was added to your cart.`));
  }

  protected toggleWishlist(product: Product): void {
    if (!this.authService.isClient() || this.actionId()) return;
    this.actionId.set(product.id);
    const removing = this.savedIds().has(product.id);
    const request = removing
      ? this.commerce.removeWishlist(product.id)
      : this.commerce.addWishlist(product.id);
    request.pipe(finalize(() => this.actionId.set(null))).subscribe((wishlist) => {
      this.savedIds.set(new Set(wishlist.items.map((item) => item.id)));
      this.notifications.show(
        removing ? 'Removed from saved products.' : 'Saved for later.',
        'info',
      );
    });
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
    document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected selectCategory(category: string, event: Event): void {
    this.filters.controls.category.setValue(category);
    this.scrollToSection(event, 'products');
  }

  protected categoryImage(category: string): string {
    const name = category.toLowerCase();
    if (/home|furniture|decor|kitchen/.test(name)) return '/assets/editorial-room.webp';
    if (/shoe|fashion|cloth|shirt|sport/.test(name)) return '/assets/editorial-sneaker.webp';
    return '/assets/editorial-headphones.webp';
  }

  protected clearFilters(): void {
    this.filters.reset({ q: '', category: '', minPrice: null, maxPrice: null, sort: 'newest' });
  }

  @HostListener('window:wheel')
  @HostListener('window:touchstart')
  @HostListener('window:keydown')
  @HostListener('focusin')
  protected interruptAnimatedScroll(): void {
    this.cancelAnimatedScroll();
  }

  protected scrollToSection(event: Event, sectionId: string): void {
    event.preventDefault();
    const section = document.getElementById(sectionId);
    if (!section) return;
    this.cancelAnimatedScroll();
    history.replaceState(null, '', `#${sectionId}`);
    const start = globalThis.scrollY;
    const headerHeight = document.querySelector<HTMLElement>('.site-header')?.offsetHeight ?? 0;
    const destination =
      sectionId === 'top'
        ? 0
        : Math.max(0, start + section.getBoundingClientRect().top - headerHeight - 16);
    const distance = destination - start;
    if (Math.abs(distance) < 2) return;
    const duration = Math.min(1000, Math.max(500, Math.abs(distance) / 4));
    const startedAt = performance.now();
    const root = document.documentElement;
    this.previousScrollBehavior = root.style.scrollBehavior;
    root.style.scrollBehavior = 'auto';
    this.scrolling = true;
    const animate = (now: number): void => {
      const progress = Math.min((now - startedAt) / duration, 1);
      const eased = (1 - Math.cos(Math.PI * progress)) / 2;
      globalThis.scrollTo({ top: start + distance * eased, left: 0, behavior: 'auto' });
      if (progress < 1) this.scrollFrame = globalThis.requestAnimationFrame(animate);
      else {
        this.scrollFrame = null;
        this.restoreScrollBehavior();
      }
    };
    this.scrollFrame = globalThis.requestAnimationFrame(animate);
  }

  private load(): void {
    const version = ++this.requestVersion;
    this.loading.set(true);
    this.loadError.set(false);
    const values = this.filters.getRawValue();
    // Optional query parameters are omitted instead of sending null to the API.
    this.productService
      .search({
        q: values.q || undefined,
        category: values.category || undefined,
        minPrice: values.minPrice ?? undefined,
        maxPrice: values.maxPrice ?? undefined,
        sort: values.sort ?? 'newest',
        page: this.page(),
        size: 12,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (version === this.requestVersion) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (result) => {
          if (version !== this.requestVersion) return;
          this.products.set(result.items);
          this.categories.set(result.categories);
          this.totalItems.set(result.totalItems);
          this.totalPages.set(result.totalPages);
        },
        error: () => {
          if (version === this.requestVersion) this.loadError.set(true);
        },
      });
  }

  protected retry(): void {
    this.load();
  }

  private cancelAnimatedScroll(): void {
    if (this.scrollFrame !== null) {
      globalThis.cancelAnimationFrame(this.scrollFrame);
      this.scrollFrame = null;
    }
    this.restoreScrollBehavior();
  }

  private restoreScrollBehavior(): void {
    if (!this.scrolling) return;
    document.documentElement.style.scrollBehavior = this.previousScrollBehavior;
    this.scrolling = false;
  }
}
