/** Formatos numéricos y de fecha según el idioma de la aplicación. */
function locale(lang: string): string {
  return lang === 'ca' ? 'ca-ES' : 'es-ES';
}

export function formatMoney(value: number | null | undefined, lang: string): string {
  if (value == null) return '—';
  return new Intl.NumberFormat(locale(lang), { style: 'currency', currency: 'EUR' }).format(value);
}

export function formatNumber(value: number | null | undefined, lang: string, maxDecimals = 2): string {
  if (value == null) return '—';
  return new Intl.NumberFormat(locale(lang), { maximumFractionDigits: maxDecimals }).format(value);
}

/**
 * Unidad en plural cuando la cantidad no es 1: «4 cajas», «2 garrafas», «3 sacos».
 * Las abreviaturas (kg, l, ud, uds…) se dejan tal cual.
 */
export function unitFor(unit: string | null | undefined, quantity: number | null | undefined, lang: string): string {
  const u = (unit ?? '').trim();
  if (!u || quantity === 1 || u.length <= 3 || /[.\d]/.test(u) || /s$/i.test(u)) return u;
  if (lang === 'ca') return /a$/i.test(u) ? u.slice(0, -1) + 'es' : u + 's';
  if (/z$/i.test(u)) return u.slice(0, -1) + 'ces';
  return /[aeiouáéíóúk]$/i.test(u) ? u + 's' : u + 'es';
}

export function formatDate(value: string | null | undefined, lang: string): string {
  if (!value) return '—';
  const d = new Date(value.length === 10 ? value + 'T12:00:00' : value);
  return d.toLocaleDateString(locale(lang), { day: '2-digit', month: '2-digit', year: 'numeric' });
}
