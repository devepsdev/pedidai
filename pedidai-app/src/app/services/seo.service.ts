import { DOCUMENT, Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

/** Datos de buscadores de una página pública (en `data.seo` de la ruta). */
export interface PageSeo {
  title: string;
  description: string;
  /** false: la página existe para el usuario pero no debe aparecer en buscadores (p. ej. el login). */
  index?: boolean;
}

const SITE = 'https://pedidai.es';
const DEFAULT_TITLE = 'PedidAI';

/**
 * Título, descripción, URL canónica y robots de cada página según la ruta.
 * Google ejecuta el JavaScript: sin esto todas las rutas mostraban el título y la canónica de la portada,
 * y Google trataba «Contacto» o «Sobre nosotros» como copias de la portada.
 * Las rutas sin `seo` (panel privado, recuperar contraseña…) quedan fuera de los buscadores.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private title = inject(Title);
  private meta = inject(Meta);
  private router = inject(Router);
  private document = inject(DOCUMENT);

  init() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.apply(this.deepest(this.router.routerState.snapshot.root), e.urlAfterRedirects));
  }

  private deepest(route: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    while (route.firstChild) route = route.firstChild;
    return route;
  }

  private apply(route: ActivatedRouteSnapshot, url: string) {
    const seo = route.data['seo'] as PageSeo | undefined;
    if (!seo) {
      this.title.setTitle(DEFAULT_TITLE);
      this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
      this.setCanonical(null);
      return;
    }

    const path = url.split(/[?#]/)[0];
    const canonical = SITE + (path === '/' ? '/' : path.replace(/\/$/, ''));
    this.title.setTitle(seo.title);
    this.meta.updateTag({ name: 'description', content: seo.description });
    this.meta.updateTag({ property: 'og:title', content: seo.title });
    this.meta.updateTag({ property: 'og:description', content: seo.description });
    this.meta.updateTag({ property: 'og:url', content: canonical });
    this.meta.updateTag({ name: 'twitter:title', content: seo.title });
    this.meta.updateTag({ name: 'twitter:description', content: seo.description });

    const indexable = seo.index !== false;
    this.meta.updateTag({ name: 'robots', content: indexable ? 'index, follow' : 'noindex, follow' });
    this.setCanonical(indexable ? canonical : null);
  }

  private setCanonical(href: string | null) {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!href) {
      link?.remove();
      return;
    }
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'canonical';
      this.document.head.appendChild(link);
    }
    link.href = href;
  }
}
