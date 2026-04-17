import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DecimalPipe, DatePipe, CurrencyPipe } from '@angular/common';
import { SuperAdminService, SuperAdminDashboard, MonthlyStats } from '../../../services/super-admin.service';

@Component({
  selector: 'app-superadmin-dashboard',
  imports: [RouterLink, TranslateModule, DecimalPipe, DatePipe, CurrencyPipe],
  templateUrl: './superadmin-dashboard.html',
})
export class SuperadminDashboard implements OnInit {
  private superAdminService = inject(SuperAdminService);

  loading = signal(true);
  error = signal('');
  dashboard = signal<SuperAdminDashboard | null>(null);
  monthlyStats = signal<MonthlyStats[]>([]);

  maxOrders = computed(() => {
    const stats = this.monthlyStats();
    return stats.length ? Math.max(...stats.map(s => s.orderCount), 1) : 1;
  });

  ngOnInit() {
    this.superAdminService.getDashboard().subscribe({
      next: data => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar el dashboard');
        this.loading.set(false);
      }
    });
    this.superAdminService.getMonthlyStats().subscribe({
      next: stats => this.monthlyStats.set(stats)
    });
  }

  barHeight(count: number): string {
    const pct = this.maxOrders() > 0 ? Math.round((count / this.maxOrders()) * 100) : 0;
    return `${Math.max(pct, 4)}%`;
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-500/20 text-green-400';
      case 'INACTIVE': return 'bg-gray-500/20 text-gray-400';
      case 'SUSPENDED': return 'bg-red-500/20 text-red-400';
      default: return 'bg-yellow-500/20 text-yellow-400';
    }
  }
}
