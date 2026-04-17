import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse, PagedResponse } from '../models/shared.model';
import { SupplierResponse, SupplierRequest } from '../models/supplier.model';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private api = inject(ApiService);

  getAll(page = 0, size = 10, sortBy = 'name', sortDir = 'asc'): Observable<PagedResponse<SupplierResponse>> {
    return this.api.get<ApiResponse<PagedResponse<SupplierResponse>>>('/suppliers', { page, size, sortBy, sortDir })
      .pipe(map(r => r.data));
  }

  getByUuid(uuid: string): Observable<SupplierResponse> {
    return this.api.get<ApiResponse<SupplierResponse>>(`/suppliers/${uuid}`)
      .pipe(map(r => r.data));
  }

  search(searchText: string, page = 0, size = 10): Observable<PagedResponse<SupplierResponse>> {
    return this.api.get<ApiResponse<PagedResponse<SupplierResponse>>>('/suppliers/search', { searchText, page, size })
      .pipe(map(r => r.data));
  }

  create(data: SupplierRequest): Observable<SupplierResponse> {
    return this.api.post<ApiResponse<SupplierResponse>>('/suppliers', data)
      .pipe(map(r => r.data));
  }

  update(uuid: string, data: SupplierRequest): Observable<SupplierResponse> {
    return this.api.put<ApiResponse<SupplierResponse>>(`/suppliers/${uuid}`, data)
      .pipe(map(r => r.data));
  }

  filter(params: Record<string, string | number | undefined>): Observable<PagedResponse<SupplierResponse>> {
    return this.api.get<ApiResponse<PagedResponse<SupplierResponse>>>('/suppliers/filter', params)
      .pipe(map(r => r.data));
  }

  toggleStatus(uuid: string, isActive: boolean): Observable<SupplierResponse> {
    return this.api.patch<ApiResponse<SupplierResponse>>(`/suppliers/${uuid}/status?isActive=${isActive}`)
      .pipe(map(r => r.data));
  }
}
