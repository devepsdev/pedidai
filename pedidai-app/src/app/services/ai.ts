import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { AiOrderRequest, AiOrderResponse, AiSuggestion } from '../models/ai.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  private api = inject(ApiService);

  processOrder(request: AiOrderRequest): Observable<AiOrderResponse> {
    return this.api.aiPost<AiOrderResponse>('/process-order', request);
  }

  analyzeConsumption(): Observable<unknown> {
    return this.api.aiPost<unknown>('/analyze-consumption', {});
  }

  suggestOrders(): Observable<AiSuggestion[]> {
    return this.api.aiPost<AiSuggestion[]>('/suggest-orders', {});
  }

  comparePrices(productName: string): Observable<unknown> {
    return this.api.aiPost<unknown>('/tools/compare_prices', { productName });
  }
}
