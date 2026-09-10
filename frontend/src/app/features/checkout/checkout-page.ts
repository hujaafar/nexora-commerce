/* File purpose: Implements the address, review, and confirmation checkout wizard. */
import { CurrencyPipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CommerceService } from '../../core/services/commerce.service';
import { NotificationService } from '../../core/services/notification.service';
import { Cart, MarketplaceOrder, PaymentMethod } from '../../models/commerce.model';

@Component({
  selector: 'app-checkout-page',
  imports: [CurrencyPipe, NgTemplateOutlet, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.scss',
})
export class CheckoutPage implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly commerce = inject(CommerceService);
  private readonly notifications = inject(NotificationService);
  protected readonly cart = signal<Cart | null>(null);
  protected readonly step = signal<1 | 2 | 3>(1);
  protected readonly placing = signal(false);
  protected readonly order = signal<MarketplaceOrder | null>(null);
  protected readonly delivery = computed(() => {
    const subtotal = this.cart()?.subtotal ?? 0;
    return subtotal >= 100 || subtotal === 0 ? 0 : 4.9;
  });
  protected readonly total = computed(() => (this.cart()?.subtotal ?? 0) + this.delivery());
  protected readonly form = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.pattern(/^[+0-9 ()-]{7,24}$/)]],
    addressLine: ['', [Validators.required, Validators.maxLength(180)]],
    city: ['', [Validators.required, Validators.maxLength(80)]],
    country: ['Bahrain', [Validators.required, Validators.maxLength(80)]],
    postalCode: ['', [Validators.maxLength(20)]],
    paymentMethod: ['PAY_ON_DELIVERY' as PaymentMethod, Validators.required],
  });

  ngOnInit(): void {
    this.commerce.cart().subscribe((cart) => this.cart.set(cart));
  }

  protected review(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.cart()?.items.length) return;
    this.step.set(2);
    globalThis.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected placeOrder(): void {
    if (this.form.invalid || this.placing()) return;
    const value = this.form.getRawValue();
    this.placing.set(true);
    this.commerce
      .checkout({
        shippingAddress: {
          fullName: value.fullName,
          phone: value.phone,
          addressLine: value.addressLine,
          city: value.city,
          country: value.country,
          postalCode: value.postalCode,
        },
        paymentMethod: value.paymentMethod,
      })
      .pipe(finalize(() => this.placing.set(false)))
      .subscribe((order) => {
        this.order.set(order);
        this.step.set(3);
        this.notifications.success('Your order is confirmed.');
        globalThis.scrollTo({ top: 0, behavior: 'smooth' });
      });
  }
}
