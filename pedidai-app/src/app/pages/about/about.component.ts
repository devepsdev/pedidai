import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-about',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './about.component.html',
})
export class AboutComponent {}
