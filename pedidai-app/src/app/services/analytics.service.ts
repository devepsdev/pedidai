import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../environments/environment';

declare global {
  interface Window {
    dataLayer: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

/** Preferencias de cookies del usuario. Las técnicas no necesitan consentimiento. */
export interface CookieConsent {
  analytics: boolean;
  ads: boolean;
  date: string;
}

const STORAGE_KEY = 'cookieConsent';
/** Si cambian las categorías o los proveedores, se sube la versión para volver a preguntar. */
const CONSENT_VERSION = 2;

/**
 * Google Analytics 4 y Google Ads con Consent Mode v2 en modo básico:
 * no se carga ninguna etiqueta de Google hasta que el usuario acepta alguna categoría,
 * y solo se activa lo que ha aceptado (analítica y/o publicidad por separado).
 */
@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private loaded = false;

  /** Emite cuando hay que abrir el panel de preferencias (enlace "Configurar cookies" del pie). */
  readonly preferencesRequested$ = new Subject<void>();

  /** Preferencias guardadas, o null si el usuario aún no ha decidido. */
  getConsent(): CookieConsent | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      return parsed?.v === CONSENT_VERSION ? { analytics: !!parsed.analytics, ads: !!parsed.ads, date: parsed.date } : null;
    } catch {
      return null;
    }
  }

  saveConsent(analytics: boolean, ads: boolean): void {
    const consent = { v: CONSENT_VERSION, analytics, ads, date: new Date().toISOString() };
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(consent)); } catch { /* almacenamiento no disponible */ }

    if (this.loaded) {
      // Ya cargado: se actualiza el consentimiento (incluida la retirada)
      window.gtag?.('consent', 'update', this.consentFlags(analytics, ads));
      if (!analytics && !ads) this.deleteGoogleCookies();
    } else {
      this.init();
    }
  }

  openPreferences(): void {
    this.preferencesRequested$.next();
  }

  /** Carga Google solo si hay consentimiento previo. Seguro de llamar varias veces. */
  init(): void {
    const consent = this.getConsent();
    if (this.loaded || !consent || (!consent.analytics && !consent.ads)) return;

    const { gaId, adsId } = environment.analytics;
    const primaryId = consent.analytics && gaId ? gaId : adsId;
    if (!primaryId) return;

    window.dataLayer = window.dataLayer || [];
    // gtag debe empujar el objeto "arguments" tal cual
    window.gtag = function gtag() { window.dataLayer.push(arguments); }; // eslint-disable-line prefer-rest-params
    window.gtag('consent', 'default', { ...this.consentFlags(false, false), wait_for_update: 500 });
    window.gtag('consent', 'update', this.consentFlags(consent.analytics, consent.ads));
    window.gtag('js', new Date());
    if (consent.analytics && gaId) {
      window.gtag('config', gaId, { send_page_view: false, anonymize_ip: true });
    }
    if (consent.ads && adsId) {
      window.gtag('config', adsId);
    }

    const script = document.createElement('script');
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${primaryId}`;
    document.head.appendChild(script);
    this.loaded = true;
  }

  trackPageView(url: string): void {
    const consent = this.getConsent();
    if (!this.loaded || !consent?.analytics || !environment.analytics.gaId) return;
    window.gtag?.('event', 'page_view', { page_path: url, send_to: environment.analytics.gaId });
  }

  /** Registro completado: evento de GA4 y conversión de Google Ads (según consentimiento). */
  trackSignUp(): void {
    const consent = this.getConsent();
    if (!this.loaded || !consent) return;
    const { gaId, adsId, adsSignupLabel } = environment.analytics;
    if (consent.analytics && gaId) {
      window.gtag?.('event', 'sign_up', { method: 'email', send_to: gaId });
    }
    if (consent.ads && adsId && adsSignupLabel) {
      window.gtag?.('event', 'conversion', { send_to: `${adsId}/${adsSignupLabel}` });
    }
  }

  private consentFlags(analytics: boolean, ads: boolean) {
    const v = (granted: boolean) => (granted ? 'granted' : 'denied');
    return {
      analytics_storage: v(analytics),
      ad_storage: v(ads),
      ad_user_data: v(ads),
      ad_personalization: v(ads),
    };
  }

  /** Borra las cookies de Google (_ga, _gcl...) cuando se retira el consentimiento. */
  private deleteGoogleCookies(): void {
    const domains = ['', location.hostname, '.' + location.hostname.replace(/^www\./, '')];
    for (const cookie of document.cookie.split(';')) {
      const name = cookie.split('=')[0].trim();
      if (/^(_ga|_gid|_gat|_gcl|_gac)/.test(name)) {
        for (const d of domains) {
          document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/${d ? '; domain=' + d : ''}`;
        }
      }
    }
  }
}
