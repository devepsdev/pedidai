/** Pedido pendiente que ha preparado el asistente (aún no enviado al proveedor). */
export interface ChatOrder {
  uuid: string;
  name: string;
  status: string;
  supplier_name: string;
  total: number;
  items: { product: string; quantity: number; unit_price: number; subtotal: number }[];
}

export interface AiMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  orders?: ChatOrder[];
  unmatched?: string[];
  isError?: boolean;
}

export interface AiOrderRequest {
  source: string;
  from?: string;
  subject?: string;
  body: string;
  sourceRef?: string;
}

export interface AiOrderResponse {
  status: 'success' | 'partial' | 'not_found' | 'error';
  message: string;
  unmatched?: string[];
  orders?: ChatOrder[];
}

export interface AiSuggestion {
  product: string;
  product_uuid?: string;
  supplier: string;
  supplier_uuid?: string;
  quantity: number;
  unit?: string;
  urgency: 'high' | 'medium' | 'low';
  estimated_savings_percent?: number;
  price?: number;
  days_since_last_order?: number;
}
