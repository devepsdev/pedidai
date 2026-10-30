import { Component, inject, signal } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { AnalyticsService } from '../../services/analytics.service';

/** Tutorial en YouTube, en modo de privacidad mejorada (sin cookies hasta que se reproduce). */
const TUTORIAL_URL = 'https://www.youtube-nocookie.com/embed/ZqmVVGxOQeY?autoplay=1&rel=0';

@Component({
  selector: 'app-tutorial-video',
  imports: [TranslateModule],
  templateUrl: './tutorial-video.html',
})
export class TutorialVideo {
  private analytics = inject(AnalyticsService);

  readonly videoUrl = inject(DomSanitizer).bypassSecurityTrustResourceUrl(TUTORIAL_URL);
  readonly playing = signal(false);

  play() {
    this.playing.set(true);
    this.analytics.trackEvent('tutorial_play');
  }
}
