import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-how-it-works-section',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './how-it-works-section.html',
})
export class HowItWorksSection {}
