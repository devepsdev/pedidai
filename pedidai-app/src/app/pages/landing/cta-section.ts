import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-cta-section',
  imports: [RouterLink, TranslateModule],
  templateUrl: './cta-section.html',
})
export class CtaSection {}
