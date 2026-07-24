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
