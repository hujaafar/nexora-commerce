import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { click, fill, order } from '../../testing/marketplace-fixtures';
import { CheckoutPage } from './checkout-page';

describe('Checkout wizard', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    vi.unstubAllGlobals();
  });
  it('requires delivery details, confirms pay-on-delivery totals and blocks duplicate submission', () => {
    vi.stubGlobal('scrollTo', vi.fn());
    TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    http
      .expectOne('/api/cart')
      .flush({ items: [{ ...order.items[0], availableQuantity: 8 }], itemCount: 2, subtotal: 70 });
    fixture.detectChanges();
    click(fixture, 'Review order');
    expect(fixture.nativeElement.textContent).toContain(
      'Please complete the required delivery details',
    );
    for (const [field, value] of Object.entries(order.shippingAddress))
      fill(fixture, `[formControlName="${field}"]`, value);
    click(fixture, 'Review order');
    expect(fixture.nativeElement.textContent).toContain('Nothing is charged now');
    click(fixture, 'Edit address');
    fill(fixture, '[formControlName="city"]', 'Riffa');
    click(fixture, 'Review order');
    click(fixture, 'Confirm order');
    const checkout = http.expectOne('/api/checkout');
    expect(checkout.request.body).toMatchObject({
      paymentMethod: 'PAY_ON_DELIVERY',
      shippingAddress: { city: 'Riffa' },
    });
    expect(fixture.nativeElement.querySelector('.confirm-button').disabled).toBe(true);
    checkout.flush({ ...order, deliveryFee: 4.9, total: 74.9 });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.success-card').textContent).toContain('$74.90');
    expect(fixture.nativeElement.textContent).toContain('Track this order');
  });
});
