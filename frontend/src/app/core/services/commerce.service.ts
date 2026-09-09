/*
 * File purpose: Centralizes cart, checkout, order, analytics, and wishlist API operations.
 */
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Cart,
  CheckoutRequest,
  CustomerAnalytics,
  MarketplaceOrder,
  OrderPage,
  OrderStatus,
  SellerAnalytics,
  Wishlist,
} from '../../models/commerce.model';

@Injectable({ providedIn: 'root' })
export class CommerceService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  cart(): Observable<Cart> {
    return this.http.get<Cart>(`${this.api}/cart`);
  }

  putCartItem(productId: string, quantity: number): Observable<Cart> {
    return this.http.put<Cart>(`${this.api}/cart/items`, { productId, quantity });
  }

  removeCartItem(productId: string): Observable<Cart> {
    return this.http.delete<Cart>(`${this.api}/cart/items/${productId}`);
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(`${this.api}/cart`);
  }

  checkout(request: CheckoutRequest): Observable<MarketplaceOrder> {
    return this.http.post<MarketplaceOrder>(`${this.api}/checkout`, request);
  }

  orders(
    seller: boolean,
    filters: {
      q?: string;
      status?: OrderStatus | '';
      from?: string;
      to?: string;
      page?: number;
      size?: number;
    },
  ): Observable<OrderPage> {
    const params: Record<string, string> = {
      page: String(filters.page ?? 0),
      size: String(filters.size ?? 10),
    };
    if (filters.q?.trim()) params['q'] = filters.q.trim();
    if (filters.status) params['status'] = filters.status;
    if (filters.from) params['from'] = new Date(filters.from).toISOString();
    if (filters.to) params['to'] = new Date(`${filters.to}T23:59:59`).toISOString();
    const path = seller ? '/orders/seller' : '/orders';
    return this.http.get<OrderPage>(`${this.api}${path}`, { params });
  }

  order(id: string, seller: boolean): Observable<MarketplaceOrder> {
    const path = seller ? `/orders/seller/${id}` : `/orders/${id}`;
    return this.http.get<MarketplaceOrder>(`${this.api}${path}`);
  }

  cancelOrder(id: string): Observable<MarketplaceOrder> {
    return this.http.post<MarketplaceOrder>(`${this.api}/orders/${id}/cancel`, {});
  }

  redoOrder(id: string): Observable<MarketplaceOrder> {
    return this.http.post<MarketplaceOrder>(`${this.api}/orders/${id}/redo`, {});
  }

  removeOrder(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/orders/${id}`);
  }

  updateOrderStatus(id: string, status: OrderStatus): Observable<MarketplaceOrder> {
    return this.http.patch<MarketplaceOrder>(`${this.api}/orders/${id}/status`, { status });
  }

  customerAnalytics(): Observable<CustomerAnalytics> {
    return this.http.get<CustomerAnalytics>(`${this.api}/analytics/customer`);
  }

  sellerAnalytics(): Observable<SellerAnalytics> {
    return this.http.get<SellerAnalytics>(`${this.api}/analytics/seller`);
  }

  wishlist(): Observable<Wishlist> {
    return this.http.get<Wishlist>(`${this.api}/wishlist`);
  }

  addWishlist(productId: string): Observable<Wishlist> {
    return this.http.put<Wishlist>(`${this.api}/wishlist/${productId}`, {});
  }

  removeWishlist(productId: string): Observable<Wishlist> {
    return this.http.delete<Wishlist>(`${this.api}/wishlist/${productId}`);
  }
}
