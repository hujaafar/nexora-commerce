/* BUY-01 learning header
 * File purpose: Centralizes product API or UI state operations.
 * Learning focus: Typed HttpClient services, observables, and separation from components.
 */
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product, ProductRequest } from '../../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/products`;

  list(): Observable<Product[]> {
    return this.http.get<Product[]>(this.baseUrl);
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
