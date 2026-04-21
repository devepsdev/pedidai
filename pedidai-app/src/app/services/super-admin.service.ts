import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';

export interface SuperAdminDashboard {
  totalCompanies: number;
  activeCompanies: number;
  inactiveCompanies: number;
  suspendedCompanies: number;
  pendingCompanies: number;
  totalUsers: number;
  totalAdmins: number;
  totalRegularUsers: number;
  totalOrders30d: number;
  totalSpent30d: number;
  potentialMRR: number;
  topCompanies: TopCompany[];
}

export interface TopCompany {
  companyUuid: string;
  companyName: string;
  orderCount: number;
  totalSpent: number;
}

export interface CompanySummary {
  uuid: string;
  name: string;
  taxId: string;
  email: string;
  city: string;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED';
  userCount: number;
  orderCount: number;
  createdAt: string;
  trialEndsAt: string | null;
  lastOrderDate: string | null;
}

export interface CompanyDetail {
  uuid: string;
  name: string;
  taxId: string;
  email: string;
  phone?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  status: string;
  createdAt: string;
  trialEndsAt?: string;
  users: UserSummary[];
  supplierCount: number;
  productCount: number;
  orderCount: number;
  recentOrders: RecentOrder[];
}

export interface UserSummary {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  active: boolean;
  lastLogin?: string;
  createdAt: string;
}

export interface UserAdmin {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  active: boolean;
  companyName: string;
  companyUuid: string;
  createdAt: string;
}

export interface RecentOrder {
  uuid: string;
  supplierName: string;
  total: number;
  status: string;
  createdAt: string;
}

export interface MonthlyStats {
  month: string;
  orderCount: number;
  totalSpent: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class SuperAdminService {
  private api = inject(ApiService);

  getDashboard(): Observable<SuperAdminDashboard> {
    return this.api.get<ApiResponse<SuperAdminDashboard>>('/superadmin/dashboard').pipe(
      map(r => r.data)
    );
  }

  getCompanies(params: { search?: string; status?: string; page?: number; size?: number } = {}): Observable<PagedResponse<CompanySummary>> {
    return this.api.get<ApiResponse<PagedResponse<CompanySummary>>>('/superadmin/companies', params).pipe(
      map(r => r.data)
    );
  }

  getCompany(uuid: string): Observable<CompanyDetail> {
    return this.api.get<ApiResponse<CompanyDetail>>(`/superadmin/companies/${uuid}`).pipe(
      map(r => r.data)
    );
  }

  updateCompanyStatus(uuid: string, status: string): Observable<CompanySummary> {
    return this.api.patch<ApiResponse<CompanySummary>>(`/superadmin/companies/${uuid}/status`, { status }).pipe(
      map(r => r.data)
    );
  }

  getUsers(params: { search?: string; role?: string; companyUuid?: string; page?: number; size?: number } = {}): Observable<PagedResponse<UserAdmin>> {
    return this.api.get<ApiResponse<PagedResponse<UserAdmin>>>('/superadmin/users', params).pipe(
      map(r => r.data)
    );
  }

  getMonthlyStats(): Observable<MonthlyStats[]> {
    return this.api.get<ApiResponse<MonthlyStats[]>>('/superadmin/stats/monthly').pipe(
      map(r => r.data)
    );
  }

  extendTrial(uuid: string, months: number): Observable<CompanySummary> {
    return this.api.patch<ApiResponse<CompanySummary>>(
      `/superadmin/companies/${uuid}/extend-trial`, { months }
    ).pipe(map(r => r.data));
  }

  activatePro(uuid: string): Observable<CompanySummary> {
    return this.api.patch<ApiResponse<CompanySummary>>(
      `/superadmin/companies/${uuid}/activate`, { plan: 'PRO' }
    ).pipe(map(r => r.data));
  }
}
