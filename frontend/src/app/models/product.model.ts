/*
 * File purpose: Defines TypeScript contracts for product data.
 */
export interface Product {
  id: string;
  name: string;
  description: string;
  category: string;
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
  category: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}

export interface ProductSearchResponse {
  items: Product[];
  totalItems: number;
  page: number;
  size: number;
  totalPages: number;
  categories: string[];
}

export interface ProductSearchFilters {
  q?: string;
  category?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  sort?: 'newest' | 'price-asc' | 'price-desc' | 'name';
  page?: number;
  size?: number;
}
