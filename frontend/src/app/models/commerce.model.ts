/*
 * File purpose: Defines the TypeScript contracts for carts, checkout, orders, wishlists, and analytics.
 */
import { Product } from './product.model';

export type OrderStatus =
  'PLACED' | 'CONFIRMED' | 'PACKING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'PAY_ON_DELIVERY' | 'CARD_SIMULATED';
export type PaymentStatus = 'DUE_ON_DELIVERY' | 'AUTHORIZED' | 'CANCELLED';

export interface CartLine {
  productId: string;
  name: string;
  category: string;
  sellerId: string;
  unitPrice: number;
  quantity: number;
  availableQuantity: number;
  imageUrl: string | null;
}

export interface Cart {
  items: CartLine[];
  itemCount: number;
  subtotal: number;
  updatedAt: string;
}

export interface ShippingAddress {
  fullName: string;
  phone: string;
  addressLine: string;
  city: string;
  country: string;
  postalCode: string;
}

export interface CheckoutRequest {
  shippingAddress: ShippingAddress;
  paymentMethod: PaymentMethod;
}

export interface OrderLine {
  productId: string;
  name: string;
  category: string;
  sellerId: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  imageUrl: string | null;
}

export interface MarketplaceOrder {
  id: string;
  orderNumber: string;
  customerId: string;
  items: OrderLine[];
  shippingAddress: ShippingAddress;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  status: OrderStatus;
  subtotal: number;
  deliveryFee: number;
  total: number;
  createdAt: string;
  updatedAt: string;
  cancelledAt: string | null;
}

export interface OrderPage {
  items: MarketplaceOrder[];
  totalItems: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface ProductMetric {
  productId: string;
  name: string;
  units: number;
  amount: number;
}

export interface CategoryMetric {
  category: string;
  units: number;
  amount: number;
}

export interface CustomerAnalytics {
  totalSpent: number;
  completedOrders: number;
  purchasedUnits: number;
  mostBoughtProducts: ProductMetric[];
  topCategories: CategoryMetric[];
}

export interface SellerAnalytics {
  revenue: number;
  orderCount: number;
  unitsSold: number;
  bestSellingProducts: ProductMetric[];
}

export interface Wishlist {
  items: Product[];
  updatedAt: string;
}
