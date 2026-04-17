export interface AiMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export interface AiOrderRequest {
  source: string;
  from?: string;
  subject?: string;
  body: string;
  sourceRef?: string;
}

export interface AiOrderResponse {
  status: 'success' | 'duplicate' | 'error';
  orderUuid?: string;
  supplier?: string;
  itemsCount?: number;
  confidence?: number;
  message: string;
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
