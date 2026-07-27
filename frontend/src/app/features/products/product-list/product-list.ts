/* BUY-01 learning header
 * File purpose: Implements the products feature behavior.
 * Learning focus: Signals, API loading states, route parameters, and custom requestAnimationFrame scrolling.
 */
import { CurrencyPipe } from '@angular/common';
import {
  Component,
  computed,
  HostListener,
  inject,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../models/product.model';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductList implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  protected readonly authService = inject(AuthService);
  private scrollFrame: number | null = null;
  private scrolling = false;
  private previousScrollBehavior = '';

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly totalUnits = computed(() =>
    this.products().reduce((total, product) => total + product.quantity, 0)
  );
  protected readonly sellers = computed(
    () => new Set(this.products().map((product) => product.sellerId)).size
  );

  ngOnInit(): void {
    this.productService
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((products) => this.products.set(products));
  }

  ngOnDestroy(): void {
    this.cancelAnimatedScroll();
  }

  @HostListener('window:wheel')
  @HostListener('window:touchstart')
  protected interruptAnimatedScroll(): void {
    this.cancelAnimatedScroll();
  }

  protected scrollToSection(event: Event, sectionId: string): void {
    event.preventDefault();
    const section = document.getElementById(sectionId);
    if (!section) {
      return;
    }

    this.cancelAnimatedScroll();
    history.replaceState(null, '', `#${sectionId}`);

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      section.scrollIntoView({ behavior: 'auto', block: 'start' });
      return;
    }

    const start = window.scrollY;
    const headerHeight =
      document.querySelector<HTMLElement>('.landing-header')?.offsetHeight ?? 0;
    const destination =
      sectionId === 'top'
        ? 0
        : Math.max(
            0,
            start + section.getBoundingClientRect().top - headerHeight - 16
          );
    const distance = destination - start;

    if (Math.abs(distance) < 2) {
      return;
    }

    // Travel at a readable pace so intermediate sections stay visible.
    // Nearby sections take at least 2.2s; long journeys can take up to 8s.
    const duration = Math.min(
      8000,
      Math.max(2200, Math.abs(distance) / 0.42)
    );
    const startedAt = performance.now();
    const root = document.documentElement;
    this.previousScrollBehavior = root.style.scrollBehavior;
    root.style.scrollBehavior = 'auto';
    this.scrolling = true;

    const animate = (now: number): void => {
      const progress = Math.min((now - startedAt) / duration, 1);
      // A sine curve feels close to a person steadily scrolling the page.
      const eased = (1 - Math.cos(Math.PI * progress)) / 2;

      window.scrollTo({
        top: start + distance * eased,
        left: 0,
        behavior: 'auto'
      });

      if (progress < 1) {
        this.scrollFrame = window.requestAnimationFrame(animate);
        return;
      }

      this.scrollFrame = null;
      this.restoreScrollBehavior();
    };

    this.scrollFrame = window.requestAnimationFrame(animate);
  }

  private cancelAnimatedScroll(): void {
    if (this.scrollFrame !== null) {
      window.cancelAnimationFrame(this.scrollFrame);
      this.scrollFrame = null;
    }
    this.restoreScrollBehavior();
  }

  private restoreScrollBehavior(): void {
    if (!this.scrolling) {
      return;
    }
    document.documentElement.style.scrollBehavior =
      this.previousScrollBehavior;
    this.scrolling = false;
  }
}
