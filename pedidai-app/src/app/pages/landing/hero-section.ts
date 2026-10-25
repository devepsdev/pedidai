import { Component, ElementRef, HostListener, OnDestroy, ViewChild, afterNextRender, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';

@Component({
  selector: 'app-hero-section',
  imports: [RouterLink, TranslateModule, LinesPipe],
  templateUrl: './hero-section.html',
})
export class HeroSection implements OnDestroy {
  @ViewChild('demo') private demo?: ElementRef<HTMLVideoElement>;

  /** Desplazamiento del logo de fondo: baja más despacio que el scroll (parallax). */
  logoOffset = signal(0);
  /** El vídeo arranca sin sonido (los navegadores solo dejan reproducir así); los subtítulos van quemados. */
  muted = signal(true);
  private observer?: IntersectionObserver;

  @HostListener('window:scroll')
  onScroll() {
    if (window.scrollY > window.innerHeight * 1.5) return;
    this.logoOffset.set(window.scrollY * 0.4);
  }

  /**
   * El vídeo solo se descarga y reproduce cuando está a la vista (en el móvil queda debajo del botón)
   * y se pausa al salir. Con «reducir movimiento» no arranca solo: se queda el póster y los controles.
   */
  constructor() {
    afterNextRender(() => this.setupVideo());
  }

  private setupVideo() {
    const video = this.demo?.nativeElement;
    if (!video) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      video.controls = true;
      return;
    }
    this.observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) video.play().catch(() => (video.controls = true));
      else video.pause();
    }, { threshold: 0.35 });
    this.observer.observe(video);
  }

  /** Al activar el sonido vuelve al principio para que la locución se oiga entera. */
  toggleSound() {
    const video = this.demo?.nativeElement;
    if (!video) return;
    const unmute = this.muted();
    this.muted.set(!unmute);
    video.muted = !unmute;
    if (unmute) {
      video.currentTime = 0;
      video.play().catch(() => {});
    }
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }
}
