import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-pricing-section',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './pricing-section.html',
})
export class PricingSection {}
