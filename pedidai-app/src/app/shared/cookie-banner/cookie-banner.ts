import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-cookie-banner',
  imports: [RouterLink, TranslateModule],
  templateUrl: './cookie-banner.html',
})
export class CookieBanner {
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
  }

  reject() {
    localStorage.setItem('cookieConsent', 'rejected');
    this.visible.set(false);
  }
}
