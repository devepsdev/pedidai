import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse, PagedResponse } from '../models/shared.model';
import { ProductResponse, ProductRequest } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private api = inject(ApiService);

  getAll(page = 0, size = 10): Observable<PagedResponse<ProductResponse>> {
    return this.api.get<ApiResponse<PagedResponse<ProductResponse>>>('/products', { page, size })
      .pipe(map(r => r.data));
  }

  getByUuid(uuid: string): Observable<ProductResponse> {
    return this.api.get<ApiResponse<ProductResponse>>(`/products/${uuid}`)
      .pipe(map(r => r.data));
  }

  search(searchText: string, page = 0, size = 10): Observable<PagedResponse<ProductResponse>> {
    return this.api.get<ApiResponse<PagedResponse<ProductResponse>>>('/products/search', { searchText, page, size })
      .pipe(map(r => r.data));
  }

  filter(params: Record<string, string | number | undefined>): Observable<PagedResponse<ProductResponse>> {
    return this.api.get<ApiResponse<PagedResponse<ProductResponse>>>('/products/filter', params)
      .pipe(map(r => r.data));
  }

  create(data: ProductRequest): Observable<ProductResponse> {
    return this.api.post<ApiResponse<ProductResponse>>('/products/create', data)
      .pipe(map(r => r.data));
  }

  update(uuid: string, data: ProductRequest): Observable<ProductResponse> {
    return this.api.put<ApiResponse<ProductResponse>>(`/products/${uuid}`, data)
      .pipe(map(r => r.data));
  }

  deactivate(uuid: string): Observable<ProductResponse> {
    return this.api.patch<ApiResponse<ProductResponse>>(`/products/deactivate/${uuid}`)
      .pipe(map(r => r.data));
  }

  uploadImage(uuid: string, file: File): Observable<ProductResponse> {
    const fd = new FormData();
    fd.append('file', file);
    return this.api.postFile<ApiResponse<ProductResponse>>(`/products/${uuid}/image`, fd)
      .pipe(map(r => r.data));
  }
}
