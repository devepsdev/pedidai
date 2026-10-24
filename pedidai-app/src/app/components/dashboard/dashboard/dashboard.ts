import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ReportService } from '../../../services/report';
import { OrderService } from '../../../services/order';
import { SupplierService } from '../../../services/supplier';
import { AiService } from '../../../services/ai';
import { CompanyService } from '../../../services/company';
import { BillingService } from '../../../services/billing';
import { DashboardResponse } from '../../../models/dashboard.model';
import { OrderResponse } from '../../../models/order.model';
import { AiSuggestion } from '../../../models/ai.model';
import { MyPlan } from '../../../models/company.model';
import { PriceService } from '../../../services/price';
import { AuthService } from '../../../services/auth';
import { LanguageService } from '../../../services/language.service';
import { UserResponse } from '../../../models/user.model';
import { PriceOverview } from '../../../models/price.model';
import { formatDate, formatMoney } from '../../../shared/format';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, TranslateModule],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private reports = inject(ReportService);
  private orders = inject(OrderService);
  private supplierService = inject(SupplierService);
  private ai = inject(AiService);
  private companyService = inject(CompanyService);
  private billing = inject(BillingService);
  private priceService = inject(PriceService);
  private auth = inject(AuthService);
  private language = inject(LanguageService);
  private translate = inject(TranslateService);
  private route = inject(ActivatedRoute);

  me = signal<UserResponse | null>(this.auth.getCurrentUser());
  prices = signal<PriceOverview | null>(null);
  verificationSent = signal(false);
  /** Recién registrado: se muestra la bienvenida en los primeros pasos. */
  welcome = signal(this.route.snapshot.queryParamMap.has('welcome'));

  stats = signal<DashboardResponse | null>(null);
  recentOrders = signal<OrderResponse[]>([]);
  suggestions = signal<AiSuggestion[]>([]);
  supplierMap = signal<Record<string, string>>({});
  myPlan = signal<MyPlan | null>(null);
  /** Vuelta de la página de pago de Stripe: 'ok' o 'cancelado'. */
  paymentResult = signal(this.route.snapshot.queryParamMap.get('pago'));
  checkoutLoading = signal(false);
  checkoutError = signal('');
  /** Solo el administrador de la empresa puede contratar. */
  isAdmin = computed(() => this.me()?.role === 'ADMIN');

  loadingStats = signal(true);
  loadingOrders = signal(true);
  loadingSuggestions = signal(true);
  statsError = signal('');
  suggestionsError = signal('');

  /**
   * Primeros pasos: subir un documento del proveedor principal, añadir un segundo proveedor, ver dónde se compra
   * más barato y hacer el primer pedido por chat. Cada paso se marca solo según los datos de la cuenta.
   */
  onboarding = computed(() => {
    const p = this.prices();
    if (!p || this.loadingOrders()) return null;
    const steps = [
      { n: 1, done: p.suppliersTracked >= 1, link: '/invoices/scan' },
      { n: 2, done: p.suppliersTracked >= 2, link: '/invoices/scan' },
      { n: 3, done: p.comparable.length > 0, link: '/prices' },
      { n: 4, done: this.recentOrders().length > 0, link: '/chat' },
    ];
    const done = steps.filter(s => s.done).length;
    const current = steps.find(s => !s.done)?.n ?? null;
    return { steps, done, current, noMatch: p.suppliersTracked >= 2 && p.comparable.length === 0 };
  });
  /** Productos que otro proveedor ofrece en su tarifa más baratos de lo que se paga ahora. */
  cheaperOffers = computed(() => (this.prices()?.comparable ?? []).filter(g => {
    const cheapest = g.offers[0];
    return cheapest?.quoted && g.offers.some(o => !o.quoted && o.latestPrice > cheapest.latestPrice);
  }).length);

  /** El usuario ha ocultado los primeros pasos (solo en este navegador). */
  onboardingHidden = signal(this.readOnboardingHidden());

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
    return formatDate(plan.trialEndsAt, this.language.current());
  });

  ngOnInit() {
    this.auth.refreshMe().subscribe({ next: u => this.me.set(u), error: () => { /* se mantiene el usuario guardado */ } });
    this.priceService.overview(90).subscribe({ next: p => this.prices.set(p), error: () => { /* sin resumen de precios */ } });
    this.reports.getDashboard().subscribe({
      next: (data) => {
        this.stats.set(data ?? null);
        this.loadingStats.set(false);
      },
      error: (err) => {
        this.statsError.set(this.translate.instant('DASHBOARD.STATS_ERROR'));
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
        this.recentOrders.set(data?.content ?? []);
        this.loadingOrders.set(false);
      },
      error: (err) => {
        this.recentOrders.set([]);
        this.loadingOrders.set(false);
      }
    });

    this.loadPlan(this.paymentResult() === 'ok' ? 5 : 0);

    this.ai.suggestOrders().subscribe({
      next: (data) => {
        this.suggestions.set(Array.isArray(data) ? data : []);
        this.loadingSuggestions.set(false);
      },
      error: (err) => {
        this.suggestionsError.set(this.translate.instant('DASHBOARD.SUGGESTIONS_ERROR'));
        this.loadingSuggestions.set(false);
      }
    });
  }

  hideOnboarding() {
    this.onboardingHidden.set(true);
    try { localStorage.setItem(this.onboardingKey(), '1'); } catch { /* almacenamiento no disponible */ }
  }

  private readOnboardingHidden(): boolean {
    try { return localStorage.getItem(this.onboardingKey()) === '1'; } catch { return false; }
  }

  private onboardingKey(): string {
    return 'onboarding-hidden:' + (this.auth.getCurrentUser()?.uuid ?? '');
  }

  resendVerification() {
    this.auth.resendMyVerification().subscribe({
      next: () => this.verificationSent.set(true),
      error: () => this.verificationSent.set(true),
    });
  }

  money(v: number | null | undefined) {
    return formatMoney(v, this.language.current());
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

  /**
   * Plan de la empresa. Al volver de pagar, Stripe avisa a la API en unos segundos: se vuelve a consultar
   * hasta que el plan aparece como de pago (o se agotan los intentos).
   */
  private loadPlan(retries: number) {
    this.companyService.getMyPlan().subscribe({
      next: data => {
        this.myPlan.set(data);
        if (retries > 0 && data.trialEndsAt) {
          setTimeout(() => this.loadPlan(retries - 1), 2000);
        }
      },
      error: () => {} // silencioso, el banner simplemente no se muestra
    });
  }

  /** Lleva a la página de pago de Stripe. */
  startCheckout() {
    this.checkoutLoading.set(true);
    this.checkoutError.set('');
    this.billing.checkout().subscribe({
      next: url => window.location.href = url,
      error: err => {
        this.checkoutError.set(err.error?.message || this.translate.instant('BILLING.ERROR'));
        this.checkoutLoading.set(false);
      }
    });
  }
}
