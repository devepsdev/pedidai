import { Component, inject, input, output, ChangeDetectionStrategy } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-navbar',
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './navbar.html',
})
export class Navbar {
  private auth = inject(AuthService);
  menuToggle = output<void>();
  /** Si el menú lateral está abierto (para aria-expanded del botón). */
  menuOpen = input(false);

  private language = inject(LanguageService);
  currentLang = this.language.current;

  get user() {
    return this.auth.getCurrentUser();
  }

  logout() {
    this.auth.logout();
  }

  setLang(lang: string) {
    this.language.use(lang === 'ca' ? 'ca' : 'es');
  }
}
