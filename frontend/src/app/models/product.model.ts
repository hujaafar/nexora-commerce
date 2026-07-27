/* BUY-01 learning header
 * File purpose: Defines TypeScript contracts for product data.
 * Learning focus: End-to-end type safety between Angular and backend DTOs.
 */
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  sellerId: string;
  imageUrls: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}
