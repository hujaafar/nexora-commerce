/*
 * File purpose: Centralizes product API or UI state operations.
 */
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Product,
  ProductRequest,
  ProductSearchFilters,
  ProductSearchResponse,
} from '../../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/products`;

  list(): Observable<Product[]> {
    return this.http.get<Product[]>(this.baseUrl);
  }

  search(filters: ProductSearchFilters): Observable<ProductSearchResponse> {
    const params: Record<string, string> = {
      page: String(filters.page ?? 0),
      size: String(filters.size ?? 12),
      sort: filters.sort ?? 'newest',
    };
    if (filters.q?.trim()) params['q'] = filters.q.trim();
    if (filters.category) params['category'] = filters.category;
    if (filters.minPrice !== null && filters.minPrice !== undefined) {
      params['minPrice'] = String(filters.minPrice);
    }
    if (filters.maxPrice !== null && filters.maxPrice !== undefined) {
      params['maxPrice'] = String(filters.maxPrice);
    }
    return this.http.get<ProductSearchResponse>(`${this.baseUrl}/search`, { params });
  }

  get(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  listMine(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.baseUrl}/mine`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, request);
  }

  update(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
