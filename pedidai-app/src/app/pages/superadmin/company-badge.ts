/**
 * Etiqueta de estado de una empresa en el panel del superadministrador.
 * El campo `status` sigue en ACTIVE cuando acaba la prueba: lo que manda es `trialEndsAt`.
 */
export type CompanyBadge = 'ACTIVE' | 'TRIAL' | 'TRIAL_ENDED' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';

export function companyBadge(c: {
  status: string;
  trialEndsAt?: string | null;
  trialEnded?: boolean;
}): CompanyBadge {
  if (c.status === 'ACTIVE' && c.trialEndsAt) return c.trialEnded ? 'TRIAL_ENDED' : 'TRIAL';
  return c.status as CompanyBadge;
}

export function companyBadgeClass(badge: string): string {
  switch (badge) {
    case 'ACTIVE':
      return 'bg-green-500/20 text-green-400';
    case 'TRIAL':
      return 'bg-cyan-500/20 text-cyan-300';
    case 'TRIAL_ENDED':
      return 'bg-amber-500/20 text-amber-300';
    case 'INACTIVE':
      return 'bg-gray-500/20 text-gray-400';
    case 'SUSPENDED':
      return 'bg-red-500/20 text-red-400';
    default:
      return 'bg-yellow-500/20 text-yellow-400';
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
