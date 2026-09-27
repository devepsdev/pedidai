import { Component, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { CookieBanner } from './shared/cookie-banner/cookie-banner';
import { AnalyticsService } from './services/analytics.service';
import { LanguageService } from './services/language.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CookieBanner],
  template: `
    <router-outlet />
    <app-cookie-banner />
  `
})
export class App {
  private language = inject(LanguageService);
  private analytics = inject(AnalyticsService);
  private router = inject(Router);

  constructor() {
    // Idioma: el elegido antes o, en la primera visita, el del navegador
    this.language.init();

    // Load GA4 if user already gave consent in a previous session
    this.analytics.init();

    // Track every client-side navigation
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.analytics.trackPageView(e.urlAfterRedirects));
  }
}
