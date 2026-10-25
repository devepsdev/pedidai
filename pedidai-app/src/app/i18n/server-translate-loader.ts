import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { Observable, of } from 'rxjs';

/**
 * Textos al prerenderizar (al compilar, en Node): se leen de public/i18n del proyecto en vez de
 * pedirlos por HTTP, porque en ese momento no hay ningún servidor que los sirva.
 * Solo lo usa app.config.server.ts; en el navegador se siguen cargando de /i18n/*.json.
 */
export class ServerTranslateLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<TranslationObject> {
    const file = join(process.cwd(), 'public', 'i18n', `${lang}.json`);
    return of(JSON.parse(readFileSync(file, 'utf-8')) as TranslationObject);
  }
}
