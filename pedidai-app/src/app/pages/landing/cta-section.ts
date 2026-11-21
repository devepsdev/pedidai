import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-cta-section',
  imports: [RouterLink, TranslateModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './cta-section.html',
})
export class CtaSection {}
