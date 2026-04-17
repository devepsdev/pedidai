export type OrderStatus = 'DRAFT' | 'PENDING' | 'SENT' | 'CONFIRMED' | 'CANCELLED' | 'DELETED';

export interface OrderItemResponse {
  uuid: string;
  productUuid: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  notes?: string;
}

export interface OrderResponse {
  uuid: string;
  name: string;
  status: OrderStatus;
  totalAmount: number;
  notes?: string;
  deliveryDate?: string;
  supplierUuid: string;
  supplierName?: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt?: string;
}

export interface OrderItemRequest {
  productUuid: string;
  quantity: number;
  notes?: string;
}

export interface OrderRequest {
  supplierUuid: string;
  name: string;
  notes?: string;
  deliveryDate?: string;
  items: OrderItemRequest[];
}
