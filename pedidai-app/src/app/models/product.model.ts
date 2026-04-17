export interface ProductSupplier {
  uuid: string;
  name: string;
}

export interface ProductResponse {
  uuid: string;
  supplier: ProductSupplier;
  category?: string;
  name: string;
  description?: string;
  price: number;
  volume?: number;
  unit?: string;
  imageUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface ProductRequest {
  name: string;
  category?: string;
  description?: string;
  price: number;
  volume?: number;
  unit?: string;
  supplierUuid: string;
}
