import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';

export interface SuperAdminDashboard {
  /** Empresas clientes (sin la de la plataforma) */
  totalCompanies: number;
  /** De pago + en prueba */
  activeCompanies: number;
  paidCompanies: number;
  trialCompanies: number;
  trialEndedCompanies: number;
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
  platform: boolean;
  trialEnded: boolean;
  dataDeletionAt: string | null;
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
  trialEnded: boolean;
  dataDeletionAt?: string | null;
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
  companyStatus: string | null;
  companyTrialEnded: boolean;
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

/** Lo que manda la API (PagedResponseDTO): los totales van dentro de `pageable`. */
interface ApiPage<T> {
  content: T[];
  pageable?: { page: number; size: number; totalPages: number; totalElements: number };
}

export function toPagedResponse<T>(p: ApiPage<T>): PagedResponse<T> {
  return {
    content: p.content ?? [],
    totalElements: p.pageable?.totalElements ?? p.content?.length ?? 0,
    totalPages: p.pageable?.totalPages ?? 1,
    number: p.pageable?.page ?? 0,
    size: p.pageable?.size ?? p.content?.length ?? 0,
  };
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
    return this.api.get<ApiResponse<ApiPage<CompanySummary>>>('/superadmin/companies', params).pipe(
      map(r => toPagedResponse(r.data))
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
    return this.api.get<ApiResponse<ApiPage<UserAdmin>>>('/superadmin/users', params).pipe(
      map(r => toPagedResponse(r.data))
    );
  }

  getMonthlyStats(): Observable<MonthlyStats[]> {
    return this.api.get<ApiResponse<MonthlyStats[]>>('/superadmin/stats/monthly').pipe(
      map(r => r.data)
    );
  }

  extendTrial(uuid: string, days: number): Observable<CompanySummary> {
    return this.api.patch<ApiResponse<CompanySummary>>(
      `/superadmin/companies/${uuid}/extend-trial`, { days }
    ).pipe(map(r => r.data));
  }

  activatePro(uuid: string): Observable<CompanySummary> {
    return this.api.patch<ApiResponse<CompanySummary>>(
      `/superadmin/companies/${uuid}/activate`, { plan: 'PRO' }
    ).pipe(map(r => r.data));
  }
}
