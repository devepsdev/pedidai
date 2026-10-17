import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-pricing-section',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './pricing-section.html',
})
export class PricingSection {
  private language = inject(LanguageService);

  /** F6 («en catalán y castellano») solo se muestra en la versión en catalán. */
  readonly features = computed(() => this.language.current() === 'ca' ? [1, 2, 3, 4, 5, 6] : [1, 2, 3, 4, 5]);
}
