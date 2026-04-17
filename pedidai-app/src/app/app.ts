import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CookieBanner } from './shared/cookie-banner/cookie-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CookieBanner],
  template: `
    <router-outlet />
    <app-cookie-banner />
  `
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
