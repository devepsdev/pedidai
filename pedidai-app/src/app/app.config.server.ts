import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { provideTranslateLoader } from '@ngx-translate/core';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { ServerTranslateLoader } from './i18n/server-translate-loader';

// Prerenderizado al compilar (outputMode «static»): los textos salen de public/i18n, sin HTTP
const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    provideTranslateLoader(ServerTranslateLoader),
  ],
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
