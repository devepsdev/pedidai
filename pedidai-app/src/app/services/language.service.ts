import { DOCUMENT, Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export type AppLang = 'es' | 'ca';

/**
 * Idioma de la aplicación (castellano o catalán).
 * Un enlace con ?lang=es|ca manda y se recuerda.
 * Primera visita: el principal del navegador. Después: el que elija el usuario, que también se guarda
 * en su cuenta para que los emails y los PDF le lleguen en ese idioma.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private translate = inject(TranslateService);
  private http = inject(HttpClient);
  private document = inject(DOCUMENT);
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readonly current = signal<AppLang>('es');

  /**
   * Idioma inicial; la promesa se resuelve cuando sus textos están cargados (se llama antes de
   * pintar o hidratar la app). Al prerenderizar, siempre castellano: es la versión que se indexa.
   */
  init(): Promise<void> {
    this.translate.addLangs(['es', 'ca']);
    this.translate.setFallbackLang('es');
    if (!this.isBrowser) return this.apply('es');
    const fromUrl = this.fromUrl();
    return fromUrl ? this.use(fromUrl) : this.apply(this.stored() ?? this.fromBrowser());
  }

  /** Cambia el idioma; si hay sesión, lo guarda también en la cuenta. */
  use(lang: AppLang): Promise<void> {
    const ready = this.apply(lang);
    try { localStorage.setItem('lang', lang); } catch { /* almacenamiento no disponible */ }
    if (this.hasSession()) {
      this.http.patch(`${environment.apiUrl}/users/me/language`, null, { params: { language: lang } })
        .subscribe({ error: () => { /* no bloquea el cambio de idioma en pantalla */ } });
    }
    return ready;
  }

  /** Adopta el idioma guardado en la cuenta del usuario al iniciar sesión. */
  adoptFromAccount(lang: string | undefined): void {
    if (lang === 'es' || lang === 'ca') {
      this.apply(lang);
      try { localStorage.setItem('lang', lang); } catch { /* almacenamiento no disponible */ }
    }
  }

  private apply(lang: AppLang): Promise<void> {
    this.current.set(lang);
    this.document.documentElement.lang = lang;
    // lang-gate.js oculta la página mientras llegan los textos en catalán (el HTML prerenderizado está en castellano)
    const reveal = () => this.document.documentElement.classList.remove('lang-pending');
    return firstValueFrom(this.translate.use(lang)).then(reveal, reveal);
  }

  private stored(): AppLang | null {
    try {
      const v = localStorage.getItem('lang');
      return v === 'es' || v === 'ca' ? v : null;
    } catch {
      return null;
    }
  }

  /** Idioma forzado por enlace (?lang=ca), p. ej. desde los anuncios en catalán. */
  private fromUrl(): AppLang | null {
    const v = new URLSearchParams(location.search).get('lang');
    return v === 'es' || v === 'ca' ? v : null;
  }

  /** Solo cuenta el idioma principal del navegador: un catalán añadido como segundo idioma no basta. */
  private fromBrowser(): AppLang {
    const main = navigator.languages?.[0] ?? navigator.language;
    return main?.toLowerCase().startsWith('ca') ? 'ca' : 'es';
  }

  private hasSession(): boolean {
    try { return !!localStorage.getItem('token'); } catch { return false; }
  }
}
