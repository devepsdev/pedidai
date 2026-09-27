import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-contact',
  imports: [TranslateModule, LinesPipe],
  templateUrl: './contact.component.html',
})
export class ContactComponent {}
