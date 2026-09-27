import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-faq-section',
  imports: [TranslateModule, LinesPipe],
  templateUrl: './faq-section.html',
})
export class FaqSection {
  readonly questions = [1, 2, 3, 4, 5, 6, 7, 8];
}
