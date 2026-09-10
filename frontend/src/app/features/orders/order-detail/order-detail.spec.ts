import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { click, order } from '../../../testing/marketplace-fixtures';
import { OrderDetail } from './order-detail';

describe('Order tracking workflow', () => {
  let http: HttpTestingController;
  let sellerView = false;
  const params = new BehaviorSubject(convertToParamMap({ id: 'order-1' }));
  beforeEach(() => {
    sellerView = false;
    TestBed.configureTestingModule({
      imports: [OrderDetail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: params } },
        { provide: AuthService, useValue: { isSeller: () => sellerView } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    vi.stubGlobal(
      'confirm',
      vi.fn(() => true),
    );
  });
  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  function page() {
    const fixture = TestBed.createComponent(OrderDetail);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading order');
    http.expectOne(sellerView ? '/api/orders/seller/order-1' : '/api/orders/order-1').flush(order);
    fixture.detectChanges();
    // ActivatedRoute.paramMap stays open throughout the page lifetime.
    expect(fixture.nativeElement.textContent).not.toContain('Loading order');
    expect(fixture.nativeElement.textContent).toContain('NX-1001');
    return fixture;
  }
  it('renders the timeline after HTTP completion and cancels/removes a customer order', () => {
    const fixture = page();
    expect(fixture.nativeElement.querySelectorAll('.timeline .reached')).toHaveLength(1);
    vi.mocked(globalThis.confirm).mockReturnValueOnce(false);
    click(fixture, 'Cancel order');
    http.expectNone('/api/orders/order-1/cancel');
    click(fixture, 'Cancel order');
    http.expectOne('/api/orders/order-1/cancel').flush({ ...order, status: 'CANCELLED' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('This order was cancelled');
    expect(fixture.nativeElement.querySelector('.timeline')).toBeNull();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    click(fixture, 'Remove from history');
    http.expectOne('/api/orders/order-1').flush(null);
    expect(navigate).toHaveBeenCalledWith(['/orders']);
  });
  it('places a fresh order when the customer chooses order again', () => {
    const fixture = page();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    click(fixture, 'Order again');
    const redo = http.expectOne('/api/orders/order-1/redo');
    expect(redo.request.method).toBe('POST');
    redo.flush({ ...order, id: 'order-2', orderNumber: 'NX-1002' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('NX-1002');
    expect(navigate).toHaveBeenCalledWith(['/orders', 'order-2']);
  });
  it('offers sellers only the next allowed fulfilment transition', () => {
    sellerView = true;
    const fixture = page();
    expect(fixture.nativeElement.textContent).not.toContain('Cancel order');
    click(fixture, 'Move to Confirmed');
    const advance = http.expectOne('/api/orders/order-1/status');
    expect(advance.request.method).toBe('PATCH');
    expect(advance.request.body).toEqual({ status: 'CONFIRMED' });
    advance.flush({ ...order, status: 'CONFIRMED' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.timeline .reached')).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('Move to Packing');
  });
});
