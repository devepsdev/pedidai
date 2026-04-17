import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse, PagedResponse } from '../models/shared.model';
import { OrderResponse, OrderRequest } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private api = inject(ApiService);

  filter(params: Record<string, string | number | boolean | undefined> = {}): Observable<PagedResponse<OrderResponse>> {
    return this.api.get<ApiResponse<PagedResponse<OrderResponse>>>('/orders/list', params)
      .pipe(map(r => r.data));
  }

  getByUuid(uuid: string): Observable<OrderResponse> {
    return this.api.get<ApiResponse<OrderResponse>>(`/orders/${uuid}`)
      .pipe(map(r => r.data));
  }

  create(data: OrderRequest): Observable<OrderResponse> {
    return this.api.post<ApiResponse<OrderResponse>>('/orders/create', data)
      .pipe(map(r => r.data));
  }

  update(uuid: string, data: OrderRequest): Observable<OrderResponse> {
    return this.api.put<ApiResponse<OrderResponse>>(`/orders/update/${uuid}`, data)
      .pipe(map(r => r.data));
  }

  send(uuid: string): Observable<OrderResponse> {
    return this.api.post<ApiResponse<OrderResponse>>(`/orders/${uuid}/send`, {})
      .pipe(map(r => r.data));
  }

  cancel(uuid: string): Observable<OrderResponse> {
    return this.api.patch<ApiResponse<OrderResponse>>(`/orders/${uuid}/cancel`)
      .pipe(map(r => r.data));
  }

  delete(uuid: string): Observable<OrderResponse> {
    return this.api.patch<ApiResponse<OrderResponse>>(`/orders/delete/${uuid}`)
      .pipe(map(r => r.data));
  }
}
