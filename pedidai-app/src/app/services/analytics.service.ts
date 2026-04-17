import { Injectable } from '@angular/core';

declare let gtag: (...args: unknown[]) => void;

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly GA_ID = 'G-XXXXXXXXXX';
  private loaded = false;

  /**
   * Checks localStorage for 'accepted' consent and, if found,
   * injects the GA4 script into the DOM. Safe to call multiple times.
   */
  initAnalytics(): void {
    if (this.loaded) return;
    const consent = localStorage.getItem('cookieConsent');
    if (consent !== 'accepted') return;

    // 1. Load the gtag.js library
    const gtagScript = document.createElement('script');
    gtagScript.async = true;
    gtagScript.src = `https://www.googletagmanager.com/gtag/js?id=${this.GA_ID}`;
    document.head.appendChild(gtagScript);

    // 2. Initialize dataLayer and gtag config
    const initScript = document.createElement('script');
    initScript.text = `
      window.dataLayer = window.dataLayer || [];
      function gtag(){dataLayer.push(arguments);}
      gtag('js', new Date());
      gtag('config', '${this.GA_ID}', { send_page_view: false });
    `;
    document.head.appendChild(initScript);

    this.loaded = true;
  }

  /**
   * Sends a page_view event to GA4.
   * No-op if analytics has not been initialised yet.
   */
  trackPageView(url: string): void {
    if (!this.loaded) return;
    try {
      gtag('config', this.GA_ID, { page_path: url });
    } catch {
      // GA not fully loaded yet — ignore silently
    }
  }
}
