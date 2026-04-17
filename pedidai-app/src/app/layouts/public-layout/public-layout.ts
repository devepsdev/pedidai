import { Component, computed, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, TranslateModule],
  templateUrl: './public-layout.html',
})
export class PublicLayoutComponent {
  private router = inject(Router);
  private translate = inject(TranslateService);

  isScrolled = signal(false);
  menuOpen = signal(false);
  isLanding = signal(this.router.url === '/');
  currentLang = signal(localStorage.getItem('lang') ?? 'ca');

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

  setLang(lang: string) {
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
    this.currentLang.set(lang);
  }
}
