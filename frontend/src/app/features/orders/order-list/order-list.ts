/* File purpose: Implements searchable customer or seller order history with status/date filters. */
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CommerceService } from '../../../core/services/commerce.service';
import { OrderPage, OrderStatus } from '../../../models/commerce.model';

@Component({
  selector: 'app-order-list',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList implements OnInit {
  private readonly commerce = inject(CommerceService);
  private readonly auth = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly sellerView = this.auth.isSeller;
  protected readonly page = signal<OrderPage | null>(null);
  protected readonly loading = signal(true);
  protected readonly currentPage = signal(0);
  protected readonly statuses: Array<OrderStatus | ''> = [
    '',
    'PLACED',
    'CONFIRMED',
    'PACKING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
  ];
  protected readonly filters = this.formBuilder.nonNullable.group({
    q: [''],
    status: this.formBuilder.nonNullable.control<OrderStatus | ''>(''),
    from: [''],
    to: [''],
  });

  ngOnInit(): void {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentPage.set(0);
        this.load();
      });
    this.load();
  }

  protected go(page: number): void {
    if (page < 0 || page >= (this.page()?.totalPages ?? 0)) return;
    this.currentPage.set(page);
    this.load();
  }

  protected statusLabel(status: OrderStatus): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  private load(): void {
    this.loading.set(true);
    this.commerce
      .orders(this.sellerView(), {
        ...this.filters.getRawValue(),
        page: this.currentPage(),
        size: 8,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => this.page.set(page));
  }
}
