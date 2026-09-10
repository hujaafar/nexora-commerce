import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { asset, chooseFile, click, fill, product } from '../../../testing/marketplace-fixtures';
import { SellerDashboard } from './seller-dashboard';

describe('Seller catalog workflow', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SellerDashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    vi.stubGlobal('scrollTo', vi.fn());
    vi.stubGlobal(
      'confirm',
      vi.fn(() => true),
    );
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });
  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  function page() {
    const fixture = TestBed.createComponent(SellerDashboard);
    fixture.detectChanges();
    http.expectOne('/api/products/mine').flush([product]);
    fixture.detectChanges();
    return fixture;
  }
  it('validates input before publishing, uploads a preview, and links the stored image', () => {
    const fixture = page();
    click(fixture, 'Launch product');
    expect(fixture.nativeElement.textContent).toContain('Price must be greater than zero');
    http.expectNone('/api/products');
    fill(fixture, '#product-name', 'New lamp');
    fill(fixture, '#description', 'Handcrafted in a local studio');
    fill(fixture, '#category', 'Home');
    fill(fixture, '#price', '40');
    fill(fixture, '#quantity', '3');
    chooseFile(fixture, [new File(['png'], 'lamp.png', { type: 'image/png' })]);
    expect(fixture.nativeElement.querySelector('.pending img').src).toBe('blob:preview');
    click(fixture, 'Launch product');
    const create = http.expectOne('/api/products');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toMatchObject({
      name: 'New lamp',
      price: 40,
      quantity: 3,
      imageUrls: [],
    });
    create.flush({ ...product, id: 'new-product', name: 'New lamp' });
    const upload = http.expectOne('/api/media/images');
    expect(upload.request.body.get('productId')).toBe('new-product');
    upload.flush(asset);
    const link = http.expectOne('/api/products/new-product');
    expect(link.request.body.imageUrls).toEqual([asset.url]);
    link.flush(product);
    http.expectOne('/api/products/mine').flush([product]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#product-name').value).toBe('');
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview');
  });
  it('edits existing images, cancels editing, and respects delete confirmation', () => {
    const fixture = page();
    click(fixture, 'Edit');
    fixture.nativeElement.querySelector('[aria-label="Remove image"]').click();
    fill(fixture, '#price', '45');
    click(fixture, 'Save product changes');
    const update = http.expectOne('/api/products/product-1');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toMatchObject({ price: 45, imageUrls: [] });
    update.flush(product);
    http.expectOne('/api/products/mine').flush([product]);
    fixture.detectChanges();
    click(fixture, 'Edit');
    fixture.nativeElement.querySelector('[aria-label="Cancel editing"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('CREATE MODE');
    vi.mocked(globalThis.confirm).mockReturnValueOnce(false);
    click(fixture, 'Delete');
    http.expectNone('/api/products/product-1');
    click(fixture, 'Delete');
    http.expectOne('/api/products/product-1').flush(null);
    http.expectOne('/api/products/mine').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Your catalog is waiting');
  });
  it('rejects unsafe files and releases removed or abandoned previews', () => {
    const fixture = page();
    chooseFile(fixture, [new File(['script'], 'bad.svg', { type: 'image/svg+xml' })]);
    expect(URL.createObjectURL).not.toHaveBeenCalled();
    chooseFile(fixture, [new File(['png'], 'lamp.png', { type: 'image/png' })]);
    fixture.nativeElement.querySelector('[aria-label="Remove image"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.pending')).toBeNull();
    chooseFile(fixture, [new File(['png'], 'lamp.png', { type: 'image/png' })]);
    fixture.destroy();
    expect(URL.revokeObjectURL).toHaveBeenCalledTimes(2);
  });
});
