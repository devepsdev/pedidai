import { Component, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs';
import { CookieBanner } from './shared/cookie-banner/cookie-banner';
import { AnalyticsService } from './services/analytics.service';

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
  private analytics = inject(AnalyticsService);
  private router = inject(Router);

  constructor() {
    // Language initialisation
    const savedLang = localStorage.getItem('lang') ?? 'ca';
    this.translate.addLangs(['ca', 'es']);
    this.translate.setFallbackLang('ca');
    this.translate.use(savedLang);

    // Load GA4 if user already gave consent in a previous session
    this.analytics.initAnalytics();

    // Track every client-side navigation
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.analytics.trackPageView(e.urlAfterRedirects));
  }
}
