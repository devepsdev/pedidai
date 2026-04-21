import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';
import { CompanyResponse, CompanyRequest, MyPlan } from '../models/company.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private api = inject(ApiService);

  get(): Observable<CompanyResponse> {
    return this.api.get<ApiResponse<CompanyResponse>>('/companies')
      .pipe(map(r => r.data));
  }

  update(data: CompanyRequest): Observable<CompanyResponse> {
    return this.api.put<ApiResponse<CompanyResponse>>('/companies', data)
      .pipe(map(r => r.data));
  }

  getMyPlan(): Observable<MyPlan> {
    return this.api.get<ApiResponse<MyPlan>>('/companies/my-plan')
      .pipe(map(r => r.data));
  }
}
