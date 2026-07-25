import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { AdminService } from '../../core/services/admin.service';
import { NotificationService } from '../../core/services/notification.service';
import { MediaAsset } from '../../models/media.model';
import { Product } from '../../models/product.model';
import { UserProfile } from '../../models/user.model';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notifications = inject(NotificationService);

  protected readonly users = signal<UserProfile[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly media = signal<MediaAsset[]>([]);
  protected readonly loading = signal(true);
  protected readonly deletingId = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected deleteProduct(product: Product): void {
    if (
      this.deletingId() ||
      !window.confirm(`Remove "${product.name}" from the public marketplace?`)
    ) {
      return;
    }
    this.deletingId.set(product.id);
    this.adminService
      .deleteProduct(product.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe(() => {
        this.products.update((products) =>
          products.filter((item) => item.id !== product.id)
        );
        this.notifications.success(`${product.name} was removed.`);
      });
  }

  protected deleteMedia(asset: MediaAsset): void {
    if (
      this.deletingId() ||
      !window.confirm(`Permanently remove "${asset.originalFilename}"?`)
    ) {
      return;
    }
    this.deletingId.set(asset.id);
    this.adminService
      .deleteMedia(asset.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe(() => {
        this.media.update((media) => media.filter((item) => item.id !== asset.id));
        this.notifications.success(`${asset.originalFilename} was removed.`);
      });
  }

  protected formatBytes(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      users: this.adminService.listUsers(),
      products: this.adminService.listProducts(),
      media: this.adminService.listMedia()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ users, products, media }) => {
        this.users.set(users);
        this.products.set(products);
        this.media.set(media);
      });
  }
}
