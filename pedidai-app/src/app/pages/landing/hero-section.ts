import { Component, HostListener, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-hero-section',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './hero-section.html',
})
export class HeroSection {
  /** Desplazamiento del logo de fondo: baja más despacio que el scroll (parallax). */
  logoOffset = signal(0);

  @HostListener('window:scroll')
  onScroll() {
    if (window.scrollY > window.innerHeight * 1.5) return;
    this.logoOffset.set(window.scrollY * 0.4);
  }
}
