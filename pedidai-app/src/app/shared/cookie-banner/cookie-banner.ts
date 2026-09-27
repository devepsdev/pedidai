import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AnalyticsService } from '../../services/analytics.service';

/**
 * Banner de consentimiento (RGPD / guía de cookies de la AEPD):
 * aceptar y rechazar con el mismo peso, configuración por categorías
 * y posibilidad de cambiar la decisión en cualquier momento desde el pie.
 */
@Component({
  selector: 'app-cookie-banner',
  imports: [RouterLink, TranslateModule],
  templateUrl: './cookie-banner.html',
})
export class CookieBanner {
  private analytics = inject(AnalyticsService);

  visible = signal(false);
  configuring = signal(false);
  analyticsOn = signal(false);
  adsOn = signal(false);

  constructor() {
    const consent = this.analytics.getConsent();
    this.visible.set(!consent);
    this.analyticsOn.set(consent?.analytics ?? false);
    this.adsOn.set(consent?.ads ?? false);

    this.analytics.preferencesRequested$
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => {
        const current = this.analytics.getConsent();
        this.analyticsOn.set(current?.analytics ?? false);
        this.adsOn.set(current?.ads ?? false);
        this.configuring.set(true);
        this.visible.set(true);
      });
  }

  acceptAll() {
    this.save(true, true);
  }

  rejectAll() {
    this.save(false, false);
  }

  saveSelection() {
    this.save(this.analyticsOn(), this.adsOn());
  }

  private save(analytics: boolean, ads: boolean) {
    this.analytics.saveConsent(analytics, ads);
    this.visible.set(false);
    this.configuring.set(false);
  }
}
