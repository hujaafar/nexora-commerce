/*
 * File purpose: Verifies product detail.spec behavior.
 */
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  ParamMap,
  provideRouter
} from '@angular/router';
import { Subject } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../models/product.model';
import { ProductDetail } from './product-detail';

describe('ProductDetail', () => {
  const product: Product = {
    id: 'product-01',
    name: 'Signal Chair',
    description: 'A focused product description.',
    price: 149,
    quantity: 4,
    sellerId: 'seller-01',
    imageUrls: [],
    createdAt: '2026-07-25T00:00:00Z',
    updatedAt: '2026-07-25T00:00:00Z'
  };

  let response: Subject<Product>;
  let routeParams: Subject<ParamMap>;

  beforeEach(async () => {
    response = new Subject<Product>();
    routeParams = new Subject<ParamMap>();
    await TestBed.configureTestingModule({
      imports: [ProductDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: routeParams.asObservable()
          }
        },
        {
          provide: ProductService,
          useValue: {
            get: () => response.asObservable()
          }
        }
      ]
    }).compileComponents();
  });

  it('leaves the loading state when the product request completes', () => {
    const fixture = TestBed.createComponent(ProductDetail);

    routeParams.next(convertToParamMap({ id: product.id }));
    fixture.detectChanges();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.loading-experience')
    ).toBeTruthy();

    response.next(product);
    response.complete();
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.loading-experience')).toBeNull();
    expect(page.querySelector('.product-intro h1')?.textContent).toContain(
      product.name
    );
  });

  it('shows a useful fallback when the product request fails', () => {
    const fixture = TestBed.createComponent(ProductDetail);

    routeParams.next(convertToParamMap({ id: 'missing-product' }));
    response.error(new Error('Not found'));
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.loading-experience')).toBeNull();
    expect(page.querySelector('.detail-error h1')?.textContent).toContain(
      "isn't here"
    );
  });
});
