import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';

/**
 * Añade el token de sesión a las peticiones de la API y del asistente de IA
 * (el asistente actúa en nombre del usuario y solo ve los datos de su empresa)
 * y el idioma actual, para que las respuestas lleguen en castellano o catalán.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const isOwnBackend = req.url.startsWith(environment.apiUrl) || req.url.startsWith(environment.aiUrl);
  if (!isOwnBackend) {
    return next(req);
  }

  let token: string | null = null;
  let lang = 'es';
  try {
    token = localStorage.getItem('token');
    lang = localStorage.getItem('lang') ?? document.documentElement.lang ?? 'es';
  } catch { /* almacenamiento no disponible */ }

  const setHeaders: Record<string, string> = { 'Accept-Language': lang === 'ca' ? 'ca' : 'es' };
  if (token) {
    setHeaders['Authorization'] = `Bearer ${token}`;
  }
  return next(req.clone({ setHeaders }));
};
