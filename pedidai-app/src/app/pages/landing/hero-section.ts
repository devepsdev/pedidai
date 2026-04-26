import { Component, signal, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-hero-section',
  imports: [RouterLink, TranslateModule],
  templateUrl: './hero-section.html',
})
export class HeroSection {
  logoOffset = signal(0);

  @HostListener('window:scroll')
  onScroll() {
    this.logoOffset.set(window.scrollY * 0.4);
  }

  scrollTo(id: string, event: Event) {
    event.preventDefault();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }
}
