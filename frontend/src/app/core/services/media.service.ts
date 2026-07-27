/*
 * File purpose: Centralizes media API or UI state operations.
 */
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MediaAsset, MediaPurpose } from '../../models/media.model';

export const MAX_IMAGE_BYTES = 2 * 1024 * 1024;
export const ALLOWED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp'
];

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/media/images`;

  upload(
    file: File,
    productId?: string,
    purpose: MediaPurpose = 'PRODUCT_IMAGE'
  ): Observable<MediaAsset> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('purpose', purpose);
    if (productId) {
      formData.append('productId', productId);
    }
    return this.http.post<MediaAsset>(this.baseUrl, formData);
  }

  listMine(): Observable<MediaAsset[]> {
    return this.http.get<MediaAsset[]>(`${this.baseUrl}/mine`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  validationMessage(file: File): string | null {
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      return 'Choose a JPEG, PNG, GIF, or WebP image.';
    }
    if (file.size > MAX_IMAGE_BYTES) {
      return 'The image must be 2 MB or smaller.';
    }
    return null;
  }
}
