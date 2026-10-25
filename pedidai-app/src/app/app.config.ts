import { ApplicationConfig, inject, provideAppInitializer, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { routes } from './app.routes';
import { jwtInterceptor } from './interceptors/jwt-interceptor';
import { errorInterceptor } from './interceptors/error-interceptor';
import { provideClientHydration } from '@angular/platform-browser';
import { LanguageService } from './services/language.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor])),
    provideTranslateService({ fallbackLang: 'es' }),
    provideTranslateHttpLoader({ prefix: 'i18n/', suffix: '.json' }),
    // Las páginas públicas llegan prerenderizadas (HTML ya pintado): Angular se engancha a ese HTML.
    // Sin withEventReplay(): mete scripts en línea que la CSP de nginx (script-src 'self') bloquea.
    provideClientHydration(),
    // Idioma y textos listos antes de pintar o hidratar: sin parpadeo de claves sin traducir
    provideAppInitializer(() => inject(LanguageService).init()),
  ],
};
