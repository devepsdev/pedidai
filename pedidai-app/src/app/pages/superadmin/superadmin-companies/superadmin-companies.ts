import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { SuperAdminService, CompanySummary, PagedResponse } from '../../../services/super-admin.service';

@Component({
  selector: 'app-superadmin-companies',
  imports: [RouterLink, FormsModule, TranslateModule, DatePipe],
  templateUrl: './superadmin-companies.html',
})
export class SuperadminCompanies implements OnInit {
  private svc = inject(SuperAdminService);

  loading = signal(true);
  companies = signal<CompanySummary[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  search = signal('');
  statusFilter = signal('');

  // Plain properties for ngModel binding
  searchValue = '';
  statusFilterValue = '';

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.svc.getCompanies({
      search: this.search(),
      status: this.statusFilter(),
      page: this.currentPage(),
      size: 20
    }).subscribe({
      next: (r: PagedResponse<CompanySummary>) => {
        this.companies.set(r.content);
        this.totalElements.set(r.totalElements);
        this.totalPages.set(r.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onSearchChange(val: string) {
    this.search.set(val);
    this.applyFilters();
  }

  onStatusFilterChange(val: string) {
    this.statusFilter.set(val);
    this.applyFilters();
  }

  applyFilters() {
    this.currentPage.set(0);
    this.load();
  }

  goToPage(p: number) {
    this.currentPage.set(p);
    this.load();
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-500/20 text-green-400';
      case 'INACTIVE': return 'bg-gray-500/20 text-gray-400';
      case 'SUSPENDED': return 'bg-red-500/20 text-red-400';
      default: return 'bg-yellow-500/20 text-yellow-400';
    }
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Activa', INACTIVE: 'Inactiva', SUSPENDED: 'Suspendida', PENDING: 'Pendiente'
    };
    return map[status] ?? status;
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
