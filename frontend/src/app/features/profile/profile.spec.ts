import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { asset, chooseFile, click, fill, seller } from '../../testing/marketplace-fixtures';
import { Profile } from './profile';

describe('Profile and analytics', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Profile],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:avatar');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });
  afterEach(() => {
    http.verify();
    localStorage.clear();
    vi.restoreAllMocks();
  });
  function page(role: 'SELLER' | 'CLIENT') {
    const user = { ...seller, role };
    localStorage.setItem(
      'nexora.session',
      JSON.stringify({ accessToken: 'test-session', expiresAt: '2099-01-01T00:00:00Z', user }),
    );
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    http.expectOne('/api/me').flush(user);
    return fixture;
  }
  it('shows seller revenue, validates the name and saves a new avatar through the media API', () => {
    const fixture = page('SELLER');
    http
      .expectOne('/api/analytics/seller')
      .flush({
        revenue: 140,
        orderCount: 2,
        unitsSold: 4,
        bestSellingProducts: [
          { productId: 'product-1', name: 'Studio lamp', units: 4, amount: 140 },
        ],
      });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('$140.00');
    expect(fixture.nativeElement.querySelector('.bar-list em').style.width).toBe('100%');
    fill(fixture, '#profile-name', '');
    click(fixture, 'Save profile changes');
    http.expectNone('/api/me');
    expect(fixture.nativeElement.textContent).toContain('Name must be between');
    fill(fixture, '#profile-name', 'New studio');
    chooseFile(fixture, [new File(['bad'], 'bad.svg', { type: 'image/svg+xml' })]);
    expect(URL.createObjectURL).not.toHaveBeenCalled();
    chooseFile(fixture, [new File(['png'], 'avatar.png', { type: 'image/png' })]);
    click(fixture, 'Save profile changes');
    const upload = http.expectOne('/api/media/images');
    expect(upload.request.body.get('purpose')).toBe('AVATAR');
    upload.flush({ ...asset, purpose: 'AVATAR' });
    const update = http.expectOne('/api/me');
    expect(update.request.body).toEqual({ name: 'New studio', avatarUrl: asset.url });
    update.flush({ ...seller, name: 'New studio', avatarUrl: asset.url });
    fixture.detectChanges();
    expect(JSON.parse(localStorage.getItem('nexora.session')!).user.name).toBe('New studio');
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:avatar');
    click(fixture, 'Remove avatar');
    click(fixture, 'Save profile changes');
    const remove = http.expectOne('/api/me');
    expect(remove.request.body.avatarUrl).toBeNull();
    remove.flush(seller);
  });
  it('shows customer spending, most-bought products and categories without seller upload controls', () => {
    const fixture = page('CLIENT');
    http
      .expectOne('/api/analytics/customer')
      .flush({
        totalSpent: 75,
        completedOrders: 1,
        purchasedUnits: 2,
        mostBoughtProducts: [{ productId: 'product-1', name: 'Studio lamp', units: 2, amount: 70 }],
        topCategories: [{ category: 'Home', units: 2, amount: 70 }],
      });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('$75.00');
    expect(fixture.nativeElement.querySelector('.category-list').textContent).toContain('Home');
    expect(fixture.nativeElement.querySelector('.avatar').disabled).toBe(true);
  });
});
