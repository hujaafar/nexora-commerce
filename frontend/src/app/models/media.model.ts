/* BUY-01 learning header
 * File purpose: Defines TypeScript contracts for media data.
 * Learning focus: End-to-end type safety between Angular and backend DTOs.
 */
export type MediaPurpose = 'PRODUCT_IMAGE' | 'AVATAR';

export interface MediaAsset {
  id: string;
  originalFilename: string;
  contentType: string;
  size: number;
  productId: string | null;
  purpose: MediaPurpose;
  url: string;
  createdAt: string;
}
