import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-about',
  imports: [RouterLink, TranslateModule],
  templateUrl: './about.component.html',
})
export class AboutComponent {}
