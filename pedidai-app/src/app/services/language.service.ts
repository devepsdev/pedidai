import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments/environment';

export type AppLang = 'es' | 'ca';

/**
 * Idioma de la aplicación (castellano o catalán).
 * Un enlace con ?lang=es|ca manda y se recuerda.
 * Primera visita: el del navegador. Después: el que elija el usuario, que también se guarda
 * en su cuenta para que los emails y los PDF le lleguen en ese idioma.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private translate = inject(TranslateService);
  private http = inject(HttpClient);

  readonly current = signal<AppLang>('es');

  init(): void {
    this.translate.addLangs(['es', 'ca']);
    this.translate.setFallbackLang('es');
    const fromUrl = this.fromUrl();
    if (fromUrl) {
      this.use(fromUrl);
    } else {
      this.apply(this.stored() ?? this.fromBrowser());
    }
  }

  /** Cambia el idioma; si hay sesión, lo guarda también en la cuenta. */
  use(lang: AppLang): void {
    this.apply(lang);
    try { localStorage.setItem('lang', lang); } catch { /* almacenamiento no disponible */ }
    if (this.hasSession()) {
      this.http.patch(`${environment.apiUrl}/users/me/language`, null, { params: { language: lang } })
        .subscribe({ error: () => { /* no bloquea el cambio de idioma en pantalla */ } });
    }
  }

  /** Adopta el idioma guardado en la cuenta del usuario al iniciar sesión. */
  adoptFromAccount(lang: string | undefined): void {
    if (lang === 'es' || lang === 'ca') {
      this.apply(lang);
      try { localStorage.setItem('lang', lang); } catch { /* almacenamiento no disponible */ }
    }
  }

  private apply(lang: AppLang): void {
    this.current.set(lang);
    this.translate.use(lang);
    document.documentElement.lang = lang;
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

  private fromBrowser(): AppLang {
    const langs = navigator.languages?.length ? navigator.languages : [navigator.language];
    return langs.some(l => l?.toLowerCase().startsWith('ca')) ? 'ca' : 'es';
  }

  private hasSession(): boolean {
    try { return !!localStorage.getItem('token'); } catch { return false; }
  }
}
