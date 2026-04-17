import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class App {
  private translate = inject(TranslateService);

  constructor() {
    const savedLang = localStorage.getItem('lang') ?? 'ca';
    this.translate.addLangs(['ca', 'es']);
    this.translate.setFallbackLang('ca');
    this.translate.use(savedLang);
  }
}
