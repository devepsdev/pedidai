import { Component, inject, output, signal } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-navbar',
  imports: [TranslateModule],
  templateUrl: './navbar.html',
})
export class Navbar {
  private auth = inject(AuthService);
  private translate = inject(TranslateService);
  menuToggle = output<void>();

  currentLang = signal(localStorage.getItem('lang') ?? 'ca');

  get user() { return this.auth.getCurrentUser(); }

  logout() { this.auth.logout(); }

  setLang(lang: string) {
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
    this.currentLang.set(lang);
  }
}
