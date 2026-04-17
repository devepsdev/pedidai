import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-pricing-section',
  imports: [RouterLink, TranslateModule],
  templateUrl: './pricing-section.html',
})
export class PricingSection {}
