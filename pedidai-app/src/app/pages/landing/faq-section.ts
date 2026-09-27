import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-faq-section',
  imports: [TranslateModule],
  templateUrl: './faq-section.html',
})
export class FaqSection {
  readonly questions = [1, 2, 3, 4, 5, 6, 7, 8];
}
