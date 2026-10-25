import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * Páginas que se generan como HTML al compilar (outputMode «static»): las públicas que deben leer
 * Google, Bing, los asistentes de IA y las vistas previas de redes sin ejecutar JavaScript.
 * Son también las URLs finales y los enlaces de sitio de Google Ads; ?lang, ?gclid y ?utm_ siguen
 * funcionando porque se leen en el navegador.
 * El resto (login, panel, recuperar contraseña…) se sirve como hasta ahora: index.csr.html y se
 * pinta en el navegador.
 */
export const serverRoutes: ServerRoute[] = [
  { path: '', renderMode: RenderMode.Prerender },
  { path: 'register', renderMode: RenderMode.Prerender },
  { path: 'sobre-nosotros', renderMode: RenderMode.Prerender },
  { path: 'contacto', renderMode: RenderMode.Prerender },
  { path: 'privacidad', renderMode: RenderMode.Prerender },
  { path: 'aviso-legal', renderMode: RenderMode.Prerender },
  { path: 'cookies', renderMode: RenderMode.Prerender },
  { path: 'terminos', renderMode: RenderMode.Prerender },
  { path: '404', renderMode: RenderMode.Prerender },
  { path: '**', renderMode: RenderMode.Client },
];
