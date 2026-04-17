import { Component } from '@angular/core';
import { HeroSection } from './hero-section';
import { BenefitsSection } from './benefits-section';
import { HowItWorksSection } from './how-it-works-section';
import { FeaturesSection } from './features-section';
import { AiSection } from './ai-section';
import { PricingSection } from './pricing-section';
import { CtaSection } from './cta-section';

@Component({
  selector: 'app-landing',
  imports: [
    HeroSection,
    BenefitsSection,
    HowItWorksSection,
    FeaturesSection,
    AiSection,
    PricingSection,
    CtaSection,
  ],
  templateUrl: './landing.html',
})
export class Landing {}
