import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PriceService } from '../../services/price';
import { LanguageService } from '../../services/language.service';
import { PriceGroup, PriceOffer, PriceOverview } from '../../models/price.model';
import { formatDate, formatMoney, formatNumber } from '../../shared/format';

/** "Mis precios": dónde comprar más barato, subidas de precio y cuánto se ha pagado de más. */
@Component({
  selector: 'app-prices',
  imports: [RouterLink, TranslateModule],
  templateUrl: './prices.html',
})
export class Prices implements OnInit {
  private priceService = inject(PriceService);
  private language = inject(LanguageService);
  private translate = inject(TranslateService);
  private router = inject(Router);

  readonly periods = [30, 90, 180, 365];
  days = signal(90);
  loading = signal(true);
  error = signal('');
  overview = signal<PriceOverview | null>(null);
  showSingle = signal(false);

  isEmpty = computed(() => (this.overview()?.productsTracked ?? 0) === 0);
  onlyOneSupplier = computed(() => (this.overview()?.suppliersTracked ?? 0) === 1);

  ngOnInit() {
    this.load();
  }

  load(days = this.days()) {
    this.days.set(days);
    this.loading.set(true);
    this.error.set('');
    this.priceService.overview(days).subscribe({
      next: data => {
        this.overview.set(data);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err?.error?.message || this.translate.instant('PRICES.LOAD_ERROR'));
        this.loading.set(false);
      },
    });
  }

  /** Abre el chat con el producto ya escrito para pedirlo al más barato. */
  orderCheapest(group: PriceGroup) {
    const text = this.translate.instant('PRICES.ORDER_PREFILL', { product: group.name.toLowerCase(), unit: group.unit || '' });
    this.router.navigate(['/chat'], { queryParams: { q: text } });
  }

  /** Cuánto más caro es este proveedor que el más barato del grupo, en %. */
  extraPercent(group: PriceGroup, offer: PriceOffer): number | null {
    if (offer.cheapest || !group.cheapestPrice) return null;
    return ((offer.latestPrice - group.cheapestPrice) / group.cheapestPrice) * 100;
  }

  /**
   * Si el más barato es un precio ofrecido (tarifa) y hay un proveedor al que ya se compra más caro,
   * cuánto se ahorraría: «Pagas 1,80 € a García; Maresme te lo ofrece a 1,55 €».
   */
  quoteHint(group: PriceGroup): { offer: PriceOffer; paid: PriceOffer; pct: number } | null {
    const offer = group.offers[0];
    const paid = group.offers.find(o => !o.quoted);
    if (!offer?.quoted || !paid || paid.latestPrice <= offer.latestPrice) return null;
    return { offer, paid, pct: ((paid.latestPrice - offer.latestPrice) / paid.latestPrice) * 100 };
  }

  money(v: number | null | undefined) {
    return formatMoney(v, this.language.current());
  }

  num(v: number | null | undefined, decimals = 1) {
    return formatNumber(v, this.language.current(), decimals);
  }

  date(v: string | null | undefined) {
    return formatDate(v, this.language.current());
  }
}
