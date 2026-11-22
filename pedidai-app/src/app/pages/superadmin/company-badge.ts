/**
 * Etiqueta de estado de una empresa en el panel del superadministrador.
 * El campo `status` sigue en ACTIVE cuando acaba la prueba: lo que manda es `trialEndsAt`.
 */
export type CompanyBadge = 'PLATFORM' | 'PAID' | 'ACTIVE' | 'TRIAL' | 'TRIAL_ENDED' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';

export function companyBadge(c: {
  status: string;
  trialEndsAt?: string | null;
  trialEnded?: boolean;
  platform?: boolean;
}): CompanyBadge {
  if (c.platform) return 'PLATFORM';
  if (c.status === 'ACTIVE' && c.trialEndsAt) return c.trialEnded ? 'TRIAL_ENDED' : 'TRIAL';
  // Sin fecha de prueba y sin ser la plataforma: cliente de pago
  if (c.status === 'ACTIVE' && c.platform === false) return 'PAID';
  return c.status as CompanyBadge;
}

export function companyBadgeClass(badge: string): string {
  switch (badge) {
    case 'PLATFORM':
      return 'bg-amber-50 text-amber-800 ring-amber-600/25';
    case 'PAID':
    case 'ACTIVE':
      return 'bg-emerald-50 text-emerald-700 ring-emerald-600/20';
    case 'TRIAL':
      return 'bg-sky-50 text-sky-700 ring-sky-600/20';
    case 'TRIAL_ENDED':
      return 'bg-amber-50 text-amber-800 ring-amber-600/25';
    case 'INACTIVE':
      return 'bg-gray-100 text-gray-600 ring-gray-500/20';
    case 'SUSPENDED':
      return 'bg-red-50 text-red-700 ring-red-600/20';
    default:
      return 'bg-yellow-50 text-yellow-800 ring-yellow-600/25';
  }
}

/**
 * Si un usuario puede entrar: OK, desactivado o bloqueado por su empresa
 * (prueba terminada, inactiva o suspendida).
 */
export type UserAccess = 'OK' | 'DISABLED' | 'TRIAL_ENDED' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';

export function userAccess(u: {
  active: boolean;
  companyStatus?: string | null;
  companyTrialEnded?: boolean;
}): UserAccess {
  if (!u.active) return 'DISABLED';
  if (u.companyStatus && u.companyStatus !== 'ACTIVE') return u.companyStatus as UserAccess;
  if (u.companyTrialEnded) return 'TRIAL_ENDED';
  return 'OK';
}

export function roleBadgeClass(role: string): string {
  switch (role) {
    case 'SUPER_ADMIN':
      return 'bg-amber-50 text-amber-800 ring-amber-600/25';
    case 'ADMIN':
      return 'bg-blue-50 text-blue-700 ring-blue-600/20';
    default:
      return 'bg-gray-100 text-gray-600 ring-gray-500/20';
  }
}

/** Dos iniciales para el avatar: «La mejillonera» → «LM», «Alex» → «AL». */
export function initials(name: string | null | undefined): string {
  const words = (name ?? '').trim().split(/\s+/).filter(Boolean);
  if (!words.length) return '?';
  const two = words.length > 1 ? words[0][0] + words[1][0] : words[0].slice(0, 2);
  return two.toUpperCase();
}

const AVATAR_COLORS = [
  'bg-blue-100 text-blue-700',
  'bg-emerald-100 text-emerald-700',
  'bg-violet-100 text-violet-700',
  'bg-amber-100 text-amber-800',
  'bg-rose-100 text-rose-700',
  'bg-sky-100 text-sky-700',
];

/** Color estable por nombre: la misma empresa siempre sale del mismo color. */
export function avatarClass(name: string | null | undefined): string {
  let h = 0;
  for (const ch of name ?? '') h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return AVATAR_COLORS[h % AVATAR_COLORS.length];
}
