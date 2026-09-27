import { Component, inject, output, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-navbar',
  imports: [TranslateModule],
  templateUrl: './navbar.html',
})
export class Navbar {
  private auth = inject(AuthService);
  menuToggle = output<void>();

  private language = inject(LanguageService);
  currentLang = this.language.current;

  get user() { return this.auth.getCurrentUser(); }

  logout() { this.auth.logout(); }

  setLang(lang: string) {
    this.language.use(lang === 'ca' ? 'ca' : 'es');
  }
}
