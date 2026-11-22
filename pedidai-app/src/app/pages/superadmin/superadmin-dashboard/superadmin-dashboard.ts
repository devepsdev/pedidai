import { Component, inject, signal, computed, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  SuperAdminService,
  SuperAdminDashboard,
  MonthlyStats,
  CompanySummary,
} from '../../../services/super-admin.service';
import { SuperadminHeader } from '../superadmin-header/superadmin-header';
import { LanguageService } from '../../../services/language.service';
import { formatMoney } from '../../../shared/format';
import { avatarClass, companyBadge, companyBadgeClass, initials } from '../company-badge';

interface Segment {
  key: string;
  label: string;
  n: number;
  bar: string;
  dot: string;
}

@Component({
  selector: 'app-superadmin-dashboard',
  imports: [RouterLink, TranslateModule, DatePipe, SuperadminHeader],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './superadmin-dashboard.html',
})
export class SuperadminDashboard implements OnInit {
  private translate = inject(TranslateService);
  private superAdminService = inject(SuperAdminService);
  private language = inject(LanguageService);

  loading = signal(true);
  error = signal('');
  dashboard = signal<SuperAdminDashboard | null>(null);
  monthlyStats = signal<MonthlyStats[]>([]);
  recent = signal<CompanySummary[]>([]);

  readonly badge = companyBadge;
  readonly badgeClass = companyBadgeClass;
  readonly initials = initials;
  readonly avatarClass = avatarClass;

  maxOrders = computed(() => {
    const stats = this.monthlyStats();
    return stats.length ? Math.max(...stats.map((s) => s.orderCount), 1) : 1;
  });

  /** Reparto de las empresas clientes por estado (para la barra apilada). */
  segments = computed<Segment[]>(() => {
    const d = this.dashboard();
    if (!d) return [];
    return [
      { key: 'paid', label: 'SUPERADMIN.STATUS.PAID', n: d.paidCompanies, bar: 'bg-emerald-500', dot: 'bg-emerald-500' },
      { key: 'trial', label: 'SUPERADMIN.STATUS.TRIAL', n: d.trialCompanies, bar: 'bg-sky-500', dot: 'bg-sky-500' },
      { key: 'ended', label: 'SUPERADMIN.STATUS.TRIAL_ENDED', n: d.trialEndedCompanies, bar: 'bg-amber-400', dot: 'bg-amber-400' },
      { key: 'inactive', label: 'SUPERADMIN.DASHBOARD.INACTIVE_PLURAL', n: d.inactiveCompanies, bar: 'bg-gray-300', dot: 'bg-gray-300' },
      { key: 'suspended', label: 'SUPERADMIN.DASHBOARD.SUSPENDED_PLURAL', n: d.suspendedCompanies, bar: 'bg-red-400', dot: 'bg-red-400' },
    ];
  });

  segmentTotal = computed(() => this.segments().reduce((sum, s) => sum + s.n, 0));

  ngOnInit() {
    this.superAdminService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('ERRORS.LOAD_DASHBOARD'));
        this.loading.set(false);
      },
    });
    this.superAdminService.getMonthlyStats().subscribe({
      next: (stats) => this.monthlyStats.set(stats),
    });
    this.superAdminService.getCompanies({ page: 0, size: 6 }).subscribe({
      next: (r) => this.recent.set(r.content.filter((c) => !c.platform).slice(0, 5)),
    });
  }

  barHeight(count: number): string {
    const pct = this.maxOrders() > 0 ? Math.round((count / this.maxOrders()) * 100) : 0;
    return `${Math.max(pct, 4)}%`;
  }

  money(v: number | null | undefined): string {
    return formatMoney(v, this.language.current());
  }

  /** «2026-11» → «nov 26» (o «de nov. 26» en catalán, según el navegador). */
  monthLabel(month: string): string {
    const [y, m] = month.split('-').map(Number);
    if (!y || !m) return month;
    const locale = this.language.current() === 'ca' ? 'ca-ES' : 'es-ES';
    return new Intl.DateTimeFormat(locale, { month: 'short', year: '2-digit' }).format(new Date(y, m - 1, 1));
  }

  segmentWidth(n: number): number {
    const total = this.segmentTotal();
    return total ? (n / total) * 100 : 0;
  }
}
