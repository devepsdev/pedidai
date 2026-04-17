import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-cookie-banner',
  imports: [RouterLink, TranslateModule],
  templateUrl: './cookie-banner.html',
})
export class CookieBanner {
  private analytics = inject(AnalyticsService);
  visible = signal(false);

  constructor() {
    const consent = localStorage.getItem('cookieConsent');
    if (!consent) {
      this.visible.set(true);
    }
  }

  accept() {
    localStorage.setItem('cookieConsent', 'accepted');
    this.visible.set(false);
    this.analytics.initAnalytics(); // Load GA4 immediately, no page reload needed
  }

  reject() {
    localStorage.setItem('cookieConsent', 'rejected');
    this.visible.set(false);
    // GA4 is never loaded
  }
}
