import { CurrencyPipe } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  ViewChild
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
export class ProductList implements OnInit, AfterViewInit, OnDestroy {
  private readonly productService = inject(ProductService);
  protected readonly authService = inject(AuthService);

  @ViewChild('marqueeSection')
  private marqueeSection?: ElementRef<HTMLElement>;

  @ViewChild('aboutCopy')
  private aboutCopy?: ElementRef<HTMLElement>;

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly marqueeOffset = signal(-200);
  protected readonly aboutProgress = signal(0);
  protected readonly magnetTransform = signal('translate3d(0, 0, 0)');

  protected readonly aboutText =
    'BUY/01 is a place for original products and the people who make them. Discover fresh ideas, support independent sellers, and find something that feels genuinely different. Let’s make shopping personal again!';
  protected readonly aboutCharacters = Array.from(this.aboutText);

  protected readonly firstMarqueeRow = [
    'https://motionsites.ai/assets/hero-space-voyage-preview-eECLH3Yc.gif',
    'https://motionsites.ai/assets/hero-codenest-preview-Cgppc2qV.gif',
    'https://motionsites.ai/assets/hero-vex-ventures-preview-BczMFIiw.gif',
    'https://motionsites.ai/assets/hero-stellar-ai-v2-preview-DjvxjG3C.gif',
    'https://motionsites.ai/assets/hero-asme-preview-B_nGDnTP.gif',
    'https://motionsites.ai/assets/hero-transform-data-preview-Cx5OU29N.gif',
    'https://motionsites.ai/assets/hero-vitara-preview-Cjz2QYyU.gif',
    'https://motionsites.ai/assets/hero-terra-preview-BFjrCr7T.gif',
    'https://motionsites.ai/assets/hero-skyelite-preview-DHaZIgUv.gif',
    'https://motionsites.ai/assets/hero-aethera-preview-DknSlcTa.gif',
    'https://motionsites.ai/assets/hero-designpro-preview-D8c5_een.gif'
  ];

  protected readonly secondMarqueeRow = [
    'https://motionsites.ai/assets/hero-stellar-ai-preview-D3HL6bw1.gif',
    'https://motionsites.ai/assets/hero-xportfolio-preview-D4A8maiC.gif',
    'https://motionsites.ai/assets/hero-orbit-web3-preview-BXt4OttD.gif',
    'https://motionsites.ai/assets/hero-nexora-preview-cx5HmUgo.gif',
    'https://motionsites.ai/assets/hero-evr-ventures-preview-DZxeVFEX.gif',
    'https://motionsites.ai/assets/hero-planet-orbit-preview-DWAP8Z1P.gif',
    'https://motionsites.ai/assets/hero-new-era-preview-CocuDUm9.gif',
    'https://motionsites.ai/assets/hero-wealth-preview-B70idl_u.gif',
    'https://motionsites.ai/assets/hero-luminex-preview-CxOP7ce6.gif',
    'https://motionsites.ai/assets/hero-celestia-preview-0yO3jXO8.gif'
  ];

  protected readonly previewImages = [
    'https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260412_055344_5eff02e0-87a5-41ce-b64f-eb08da8f33db.png&w=1280&q=85',
    'https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260412_055431_11d841fd-8b41-46a5-82e4-b04f2407a7d8.png&w=1280&q=85',
    'https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260412_055451_e317bf2d-28d4-48cc-86b0-6f72f25b6327.png&w=1280&q=85'
  ];

  private animationFrame = 0;

  ngOnInit(): void {
    this.productService
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((products) => this.products.set(products));
  }

  ngAfterViewInit(): void {
    window.addEventListener('scroll', this.queueScrollUpdate, { passive: true });
    window.addEventListener('resize', this.queueScrollUpdate, { passive: true });
    this.updateScrollEffects();
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.queueScrollUpdate);
    window.removeEventListener('resize', this.queueScrollUpdate);
    cancelAnimationFrame(this.animationFrame);
  }

  protected moveMagnet(event: PointerEvent): void {
    if (event.pointerType === 'touch') {
      return;
    }

    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const x = (event.clientX - (rect.left + rect.width / 2)) / 3;
    const y = (event.clientY - (rect.top + rect.height / 2)) / 3;
    this.magnetTransform.set(`translate3d(${x}px, ${y}px, 0)`);
  }

  protected resetMagnet(): void {
    this.magnetTransform.set('translate3d(0, 0, 0)');
  }

  protected characterOpacity(index: number): number {
    const start = (index / this.aboutCharacters.length) * 0.76;
    const progress = (this.aboutProgress() - start) / 0.24;
    return Math.max(0.2, Math.min(1, 0.2 + progress * 0.8));
  }

  protected productImage(product: Product, fallbackIndex: number): string {
    return product.imageUrls[fallbackIndex] ?? product.imageUrls[0] ?? this.previewImages[fallbackIndex];
  }

  private readonly queueScrollUpdate = (): void => {
    cancelAnimationFrame(this.animationFrame);
    this.animationFrame = requestAnimationFrame(() => this.updateScrollEffects());
  };

  private updateScrollEffects(): void {
    const marquee = this.marqueeSection?.nativeElement;
    if (marquee) {
      const sectionTop = window.scrollY + marquee.getBoundingClientRect().top;
      this.marqueeOffset.set(
        (window.scrollY - sectionTop + window.innerHeight) * 0.3 - 200
      );
    }

    const copy = this.aboutCopy?.nativeElement;
    if (copy) {
      const rect = copy.getBoundingClientRect();
      const start = window.innerHeight * 0.8;
      const end = window.innerHeight * 0.2;
      const progress = (start - rect.top) / Math.max(1, start - end + rect.height);
      this.aboutProgress.set(Math.max(0, Math.min(1, progress)));
    }
  }
}
