import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse, PagedResponse } from '../models/shared.model';
import { UserResponse, UserRequest } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private api = inject(ApiService);

  getAll(page = 0, size = 10): Observable<PagedResponse<UserResponse>> {
    return this.api.get<ApiResponse<PagedResponse<UserResponse>>>('/users', { page, size })
      .pipe(map(r => r.data));
  }

  getByUuid(uuid: string): Observable<UserResponse> {
    return this.api.get<ApiResponse<UserResponse>>(`/users/${uuid}`)
      .pipe(map(r => r.data));
  }

  search(searchText: string, page = 0, size = 10): Observable<PagedResponse<UserResponse>> {
    return this.api.get<ApiResponse<PagedResponse<UserResponse>>>('/users/search', { searchText, page, size })
      .pipe(map(r => r.data));
  }

  filter(params: Record<string, string | number | undefined>): Observable<PagedResponse<UserResponse>> {
    return this.api.get<ApiResponse<PagedResponse<UserResponse>>>('/users/filter', params)
      .pipe(map(r => r.data));
  }

  create(data: UserRequest): Observable<UserResponse> {
    return this.api.post<ApiResponse<UserResponse>>('/users', data)
      .pipe(map(r => r.data));
  }

  update(uuid: string, data: UserRequest): Observable<UserResponse> {
    return this.api.put<ApiResponse<UserResponse>>(`/users/${uuid}`, data)
      .pipe(map(r => r.data));
  }

  toggleStatus(uuid: string, isActive: boolean): Observable<UserResponse> {
    return this.api.patch<ApiResponse<UserResponse>>(`/users/${uuid}/status?isActive=${isActive}`)
      .pipe(map(r => r.data));
  }
}
