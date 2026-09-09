/*
 * File purpose: Verifies that Nexora Commerce commerce actions use the correct HTTP methods, paths, and payloads.
 * New concept: HttpTestingController captures requests without needing a real backend server.
 */
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CommerceService } from './commerce.service';

describe('CommerceService', () => {
  let service: CommerceService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CommerceService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('adds a product to the cart with its requested quantity', () => {
    service.putCartItem('product-7', 3).subscribe();

    const request = http.expectOne('/api/cart/items');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ productId: 'product-7', quantity: 3 });
    request.flush({ items: [], itemCount: 0, subtotal: 0, updatedAt: '' });
  });

  it('checks out using the selected payment method and shipping address', () => {
    const checkout = {
      shippingAddress: {
        fullName: 'Mina Buyer',
        phone: '+97300000000',
        addressLine: 'Road 1',
        city: 'Manama',
        country: 'Bahrain',
        postalCode: '000',
      },
      paymentMethod: 'PAY_ON_DELIVERY' as const,
    };

    service.checkout(checkout).subscribe();

    const request = http.expectOne('/api/checkout');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(checkout);
    request.flush({});
  });

  it('sends seller order filters as query parameters', () => {
    service.orders(true, { q: 'desk', status: 'SHIPPED', page: 2, size: 5 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === '/api/orders/seller');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('q')).toBe('desk');
    expect(request.request.params.get('status')).toBe('SHIPPED');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('5');
    request.flush({ items: [], totalItems: 0, page: 2, size: 5, totalPages: 0 });
  });
});
