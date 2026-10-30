import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LinesPipe } from '../../shared/lines.pipe';
import { TutorialVideo } from './tutorial-video';

@Component({
  selector: 'app-how-it-works-section',
  imports: [RouterLink, TranslateModule, LinesPipe, TutorialVideo],
  templateUrl: './how-it-works-section.html',
})
export class HowItWorksSection {}
