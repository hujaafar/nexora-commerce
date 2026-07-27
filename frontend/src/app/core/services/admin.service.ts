/* BUY-01 learning header
 * File purpose: Centralizes admin API or UI state operations.
 * Learning focus: Typed HttpClient services, observables, and separation from components.
 */
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MediaAsset } from '../../models/media.model';
import { Product } from '../../models/product.model';
import { UserProfile } from '../../models/user.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  listUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${environment.apiUrl}/admin/users`);
  }

  listProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${environment.apiUrl}/products/moderation`);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/products/moderation/${id}`
    );
  }

  listMedia(): Observable<MediaAsset[]> {
    return this.http.get<MediaAsset[]>(
      `${environment.apiUrl}/media/images/moderation`
    );
  }

  deleteMedia(id: string): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/media/images/moderation/${id}`
    );
  }
}
