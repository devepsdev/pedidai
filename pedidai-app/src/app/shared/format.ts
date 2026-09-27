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

export function formatDate(value: string | null | undefined, lang: string): string {
  if (!value) return '—';
  const d = new Date(value.length === 10 ? value + 'T12:00:00' : value);
  return d.toLocaleDateString(locale(lang), { day: '2-digit', month: '2-digit', year: 'numeric' });
}
