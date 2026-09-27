import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';
import { formatMoney } from '../../shared/format';
import { LinesPipe } from '../../shared/lines.pipe';

/**
 * Ejemplo de ahorro con cifras supuestas y explícitas (no son resultados de clientes):
 * parte de las compras que tiene un proveedor más barato y la diferencia media de precio.
 */
@Component({
  selector: 'app-savings-section',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './savings-section.html',
})
export class SavingsSection {
  private language = inject(LanguageService);

  /** Hipótesis del ejemplo, visibles en la página. */
  readonly sharePercent = 30;
  readonly gapPercent = 8;
  readonly launchPrice = 29;

  monthly = signal(6000);

  comparable = computed(() => this.monthly() * this.sharePercent / 100);
  saving = computed(() => Math.round(this.comparable() * this.gapPercent / 100));
  net = computed(() => this.saving() - this.launchPrice);

  money(v: number) {
    return formatMoney(v, this.language.current()).replace(/[,.]00(?=\s?€)/, '');
  }
}
