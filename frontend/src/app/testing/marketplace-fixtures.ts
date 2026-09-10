import { ComponentFixture } from '@angular/core/testing';
import { MarketplaceOrder } from '../models/commerce.model';

export const product = {
  id: 'product-1',
  name: 'Studio lamp',
  description: 'Handmade desk lamp',
  category: 'Home',
  price: 35,
  quantity: 8,
  imageUrls: ['/media/images/photo-1'],
  sellerId: 'seller-1',
  createdAt: '2026-01-15T12:00:00Z',
  updatedAt: '2026-01-15T12:00:00Z',
};
export const seller = {
  id: 'seller-1',
  name: 'Studio seller',
  email: 'seller@example.com',
  role: 'SELLER' as const,
  avatarUrl: null,
  createdAt: '2026-01-15T12:00:00Z',
};
export const asset = {
  id: 'photo-1',
  url: '/media/images/photo-1',
  originalFilename: 'lamp.png',
  contentType: 'image/png',
  size: 1024,
  ownerId: 'seller-1',
  productId: 'product-1',
  purpose: 'PRODUCT_IMAGE' as const,
  createdAt: '2026-01-15T12:00:00Z',
};
export const order: MarketplaceOrder = {
  id: 'order-1',
  orderNumber: 'NX-1001',
  customerId: 'customer-1',
  items: [
    {
      productId: product.id,
      name: product.name,
      category: product.category,
      sellerId: seller.id,
      unitPrice: 35,
      quantity: 2,
      lineTotal: 70,
      imageUrl: null,
    },
  ],
  shippingAddress: {
    fullName: 'Test customer',
    phone: '+97312345678',
    addressLine: '15 Test Street',
    city: 'Manama',
    country: 'Bahrain',
    postalCode: '123',
  },
  paymentMethod: 'PAY_ON_DELIVERY',
  paymentStatus: 'DUE_ON_DELIVERY',
  status: 'PLACED',
  subtotal: 70,
  deliveryFee: 5,
  total: 75,
  createdAt: '2026-01-15T12:00:00Z',
  updatedAt: '2026-01-15T12:00:00Z',
  cancelledAt: null,
};
export function fill<T>(fixture: ComponentFixture<T>, selector: string, value: string): void {
  const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input', { bubbles: true }));
  fixture.detectChanges();
}
export function click<T>(fixture: ComponentFixture<T>, text: string): void {
  const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
  const button = buttons.find((item) => item.textContent?.includes(text));
  if (!button) throw new Error(`Button missing: ${text}`);
  button.click();
  fixture.detectChanges();
}
export function chooseFile<T>(fixture: ComponentFixture<T>, files: File[]): void {
  const input = fixture.nativeElement.querySelector('input[type=file]') as HTMLInputElement;
  Object.defineProperty(input, 'files', { configurable: true, value: files });
  input.dispatchEvent(new Event('change', { bubbles: true }));
  fixture.detectChanges();
}
