import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ReportService } from '../../../services/report';
import { OrderService } from '../../../services/order';
import { SupplierService } from '../../../services/supplier';
import { AiService } from '../../../services/ai';
import { CompanyService } from '../../../services/company';
import { DashboardResponse } from '../../../models/dashboard.model';
import { OrderResponse } from '../../../models/order.model';
import { AiSuggestion } from '../../../models/ai.model';
import { MyPlan } from '../../../models/company.model';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe, DatePipe, TranslateModule],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private reports = inject(ReportService);
  private orders = inject(OrderService);
  private supplierService = inject(SupplierService);
  private ai = inject(AiService);
  private companyService = inject(CompanyService);

  stats = signal<DashboardResponse | null>(null);
  recentOrders = signal<OrderResponse[]>([]);
  suggestions = signal<AiSuggestion[]>([]);
  supplierMap = signal<Record<string, string>>({});
  myPlan = signal<MyPlan | null>(null);

  loadingStats = signal(true);
  loadingOrders = signal(true);
  loadingSuggestions = signal(true);
  statsError = signal('');
  suggestionsError = signal('');

  /** Días restantes de trial. null = sin trial (cliente de pago o no cargado aún). */
  trialDaysLeft = computed(() => {
    const plan = this.myPlan();
    if (!plan?.trialEndsAt) return null;
    const end = new Date(plan.trialEndsAt);
    const now = new Date();
    const days = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return days > 0 ? days : 0;
  });

  /** Fecha de fin de trial formateada (dd/MM/yyyy). */
  trialEndDate = computed(() => {
    const plan = this.myPlan();
    if (!plan?.trialEndsAt) return null;
    const d = new Date(plan.trialEndsAt);
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
  });

  ngOnInit() {
    this.reports.getDashboard().subscribe({
      next: (data) => {
        console.log('Dashboard stats:', data);
        this.stats.set(data ?? null);
        this.loadingStats.set(false);
      },
      error: (err) => {
        console.error('Dashboard stats error:', err);
        this.statsError.set('Error al cargar estadísticas');
        this.loadingStats.set(false);
      }
    });

    this.supplierService.getAll(0, 200).subscribe({
      next: (data) => {
        const map: Record<string, string> = {};
        data.content.forEach(s => (map[s.uuid] = s.name));
        this.supplierMap.set(map);
      },
      error: () => {},
    });

    this.orders.filter({ size: 5, sortBy: 'createdAt', sortDir: 'desc' }).subscribe({
      next: (data) => {
        console.log('Recent orders:', data);
        this.recentOrders.set(data?.content ?? []);
        this.loadingOrders.set(false);
      },
      error: (err) => {
        console.error('Recent orders error:', err);
        this.recentOrders.set([]);
        this.loadingOrders.set(false);
      }
    });

    this.companyService.getMyPlan().subscribe({
      next: data => this.myPlan.set(data),
      error: () => {} // silencioso, el banner simplemente no se muestra
    });

    this.ai.suggestOrders().subscribe({
      next: (data) => {
        console.log('AI suggestions:', data);
        this.suggestions.set(Array.isArray(data) ? data : []);
        this.loadingSuggestions.set(false);
      },
      error: (err) => {
        console.error('AI suggestions error:', err);
        this.suggestionsError.set('Servicio IA no disponible');
        this.loadingSuggestions.set(false);
      }
    });
  }

  getSupplierName(uuid: string): string {
    return this.supplierMap()[uuid] ?? '—';
  }

  urgencyClass(urgency: string): string {
    return urgency === 'high' ? 'bg-red-100 text-red-700'
      : urgency === 'medium' ? 'bg-orange-100 text-orange-700'
      : 'bg-green-100 text-green-700';
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'bg-gray-100 text-gray-600',
      PENDING: 'bg-yellow-100 text-yellow-700',
      SENT: 'bg-blue-100 text-blue-700',
      CONFIRMED: 'bg-green-100 text-green-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }
}
