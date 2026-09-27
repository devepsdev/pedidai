import { Component } from '@angular/core';
import { HeroSection } from './hero-section';
import { HowItWorksSection } from './how-it-works-section';
import { SavingsSection } from './savings-section';
import { PricingSection } from './pricing-section';
import { FaqSection } from './faq-section';
import { CtaSection } from './cta-section';

@Component({
  selector: 'app-landing',
  imports: [HeroSection, HowItWorksSection, SavingsSection, PricingSection, FaqSection, CtaSection],
  templateUrl: './landing.html',
})
export class Landing {}
