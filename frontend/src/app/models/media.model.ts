/*
 * File purpose: Defines TypeScript contracts for media data.
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
