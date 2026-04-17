import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';
import { DashboardResponse, GlobalReport } from '../models/dashboard.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private api = inject(ApiService);
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  getDashboard(): Observable<DashboardResponse> {
    return this.api.get<ApiResponse<DashboardResponse>>('/reports/dashboard')
      .pipe(map(r => r.data));
  }

  getGlobal(startDate: string, endDate: string): Observable<GlobalReport> {
    return this.api.get<ApiResponse<GlobalReport>>('/reports/global', { startDate, endDate })
      .pipe(map(r => r.data));
  }

  getGlobalPdf(startDate: string, endDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get(`${this.baseUrl}/reports/global/pdf`, { params, responseType: 'blob' });
  }
}
