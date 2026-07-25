import { CurrencyPipe } from '@angular/common';
import {
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { MediaService } from '../../../core/services/media.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { Product, ProductRequest } from '../../../models/product.model';

interface PendingImage {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-seller-dashboard',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.scss'
})
export class SellerDashboard implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);
  private readonly notifications = inject(NotificationService);

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly editingProduct = signal<Product | null>(null);
  protected readonly pendingImages = signal<PendingImage[]>([]);
  protected readonly existingImageUrls = signal<string[]>([]);
  protected readonly totalUnits = computed(() =>
    this.products().reduce((total, product) => total + product.quantity, 0)
  );
  protected readonly totalImages = computed(() =>
    this.products().reduce(
      (total, product) => total + product.imageUrls.length,
      0
    )
  );
  protected readonly catalogValue = computed(() =>
    this.products().reduce(
      (total, product) => total + product.price * product.quantity,
      0
    )
  );

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    price: [0, [Validators.required, Validators.min(0.01)]],
    quantity: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.loadProducts();
  }

  ngOnDestroy(): void {
    this.revokePendingPreviews();
  }

  protected chooseImages(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    for (const file of files) {
      const message = this.mediaService.validationMessage(file);
      if (message) {
        this.notifications.error(`${file.name}: ${message}`);
        continue;
      }
      if (
        this.pendingImages().length + this.existingImageUrls().length >=
        8
      ) {
        this.notifications.error('A product can have at most 8 images.');
        break;
      }
      this.pendingImages.update((images) => [
        ...images,
        { file, previewUrl: URL.createObjectURL(file) }
      ]);
    }
    input.value = '';
  }

  protected removePending(index: number): void {
    const image = this.pendingImages()[index];
    if (image) {
      URL.revokeObjectURL(image.previewUrl);
    }
    this.pendingImages.update((images) =>
      images.filter((_, currentIndex) => currentIndex !== index)
    );
  }

  protected removeExisting(url: string): void {
    this.existingImageUrls.update((urls) => urls.filter((item) => item !== url));
  }

  protected edit(product: Product): void {
    this.revokePendingPreviews();
    this.editingProduct.set(product);
    this.existingImageUrls.set([...product.imageUrls]);
    this.form.setValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelEdit(): void {
    this.resetEditor();
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    const editing = this.editingProduct();
    const baseRequest: ProductRequest = {
      ...this.form.getRawValue(),
      imageUrls: [...this.existingImageUrls()]
    };

    const operation = editing
      ? this.updateProduct(editing.id, baseRequest)
      : this.createProduct(baseRequest);

    operation.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (product) => {
        this.notifications.success(
          editing ? `${product.name} was updated.` : `${product.name} was published.`
        );
        this.resetEditor();
        this.loadProducts();
      },
      error: () => this.loadProducts()
    });
  }

  protected delete(product: Product): void {
    if (!window.confirm(`Delete “${product.name}”? This cannot be undone.`)) {
      return;
    }
    this.productService.delete(product.id).subscribe(() => {
      this.notifications.success(`${product.name} was deleted.`);
      if (this.editingProduct()?.id === product.id) {
        this.resetEditor();
      }
      this.loadProducts();
    });
  }

  private createProduct(request: ProductRequest): Observable<Product> {
    return this.productService
      .create({ ...request, imageUrls: [] })
      .pipe(
        switchMap((created) =>
          this.uploadPending(created.id).pipe(
            switchMap((newUrls) =>
              newUrls.length
                ? this.productService.update(created.id, {
                    ...request,
                    imageUrls: newUrls
                  })
                : of(created)
            )
          )
        )
      );
  }

  private updateProduct(
    productId: string,
    request: ProductRequest
  ): Observable<Product> {
    return this.uploadPending(productId).pipe(
      switchMap((newUrls) =>
        this.productService.update(productId, {
          ...request,
          imageUrls: [...request.imageUrls, ...newUrls]
        })
      )
    );
  }

  private uploadPending(productId: string): Observable<string[]> {
    const uploads = this.pendingImages().map((image) =>
      this.mediaService
        .upload(image.file, productId, 'PRODUCT_IMAGE')
        .pipe(map((asset) => asset.url))
    );
    return uploads.length ? forkJoin(uploads) : of([]);
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.productService
      .listMine()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((products) => this.products.set(products));
  }

  private resetEditor(): void {
    this.revokePendingPreviews();
    this.editingProduct.set(null);
    this.existingImageUrls.set([]);
    this.form.reset({
      name: '',
      description: '',
      price: 0,
      quantity: 0
    });
  }

  private revokePendingPreviews(): void {
    this.pendingImages().forEach((image) =>
      URL.revokeObjectURL(image.previewUrl)
    );
    this.pendingImages.set([]);
  }
}
