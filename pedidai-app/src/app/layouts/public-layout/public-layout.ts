import { Component, computed, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';
import { AnalyticsService } from '../../services/analytics.service';
import { filter } from 'rxjs';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, TranslateModule, LinesPipe],
  templateUrl: './public-layout.html',
})
export class PublicLayoutComponent {
  private router = inject(Router);

  isScrolled = signal(false);
  menuOpen = signal(false);
  isLanding = signal(this.router.url === '/');
  private language = inject(LanguageService);
  private analytics = inject(AnalyticsService);
  readonly year = new Date().getFullYear();
  currentLang = this.language.current;

  navbarSolid = computed(() => !this.isLanding() || this.isScrolled());

  constructor() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => {
        this.isLanding.set(e.urlAfterRedirects === '/');
        this.menuOpen.set(false);
        setTimeout(() => this.isScrolled.set(window.scrollY > 20), 0);
      });
  }

  @HostListener('window:scroll')
  onScroll() {
    this.isScrolled.set(window.scrollY > 20);
  }

  toggleMenu() {
    this.menuOpen.update(v => !v);
  }

  closeMenu() {
    this.menuOpen.set(false);
  }

  scrollTo(id: string, event: Event) {
    event.preventDefault();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
    this.closeMenu();
  }

  openCookieSettings() {
    this.analytics.openPreferences();
  }

  setLang(lang: string) {
    this.language.use(lang === 'ca' ? 'ca' : 'es');
  }
}
