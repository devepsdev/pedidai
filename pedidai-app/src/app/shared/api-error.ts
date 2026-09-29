import { HttpErrorResponse } from '@angular/common/http';

/**
 * Mensaje para el usuario a partir de un error de la API.
 * La API responde `{ success: false, message }` con el texto ya traducido al idioma del usuario;
 * si no hay mensaje útil (sin conexión, error 5xx, respuesta inesperada) se usa el texto genérico.
 */
export function apiError(err: unknown, fallback: string): string {
  const e = err as HttpErrorResponse | undefined;
  if (!e || e.status === 0 || (e.status ?? 500) >= 500) {
    return fallback;
  }
  const message = (e.error as { message?: unknown } | null)?.message;
  // Una clave sin traducir (p. ej. «error.x.y») no le sirve al usuario
  if (typeof message !== 'string' || !message.trim() || /^[a-z]+(\.[a-zA-Z]+)+$/.test(message.trim())) {
    return fallback;
  }
  return message.trim();
}
