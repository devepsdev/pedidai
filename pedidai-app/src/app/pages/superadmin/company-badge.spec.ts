import { toPagedResponse } from '../../services/super-admin.service';
import { avatarClass, companyBadge, companyBadgeClass, initials, userAccess } from './company-badge';

describe('companyBadge', () => {
  it('prueba terminada: ACTIVE con la prueba acabada', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: '2026-11-05T11:14:25', trialEnded: true })).toBe('TRIAL_ENDED');
  });

  it('en prueba: ACTIVE con la prueba en curso', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: '2099-01-01T00:00:00', trialEnded: false })).toBe('TRIAL');
  });

  it('de pago: ACTIVE sin fecha de prueba y sin ser la plataforma', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: null, trialEnded: false, platform: false })).toBe('PAID');
  });

  it('sin saber si es la plataforma: se queda en ACTIVE', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: null, trialEnded: false })).toBe('ACTIVE');
  });

  it('cuenta de la plataforma', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: null, platform: true })).toBe('PLATFORM');
  });

  it('inactiva o suspendida: manda el estado', () => {
    expect(companyBadge({ status: 'INACTIVE', trialEndsAt: '2026-11-05T11:14:25', trialEnded: true })).toBe('INACTIVE');
    expect(companyBadge({ status: 'SUSPENDED', trialEndsAt: null })).toBe('SUSPENDED');
  });

  it('prueba terminada en ámbar', () => {
    expect(companyBadgeClass('TRIAL_ENDED')).toContain('amber');
  });
});

describe('userAccess', () => {
  it('puede entrar: activo y empresa sin bloqueo', () => {
    expect(userAccess({ active: true, companyStatus: 'ACTIVE', companyTrialEnded: false })).toBe('OK');
  });

  it('empresa con la prueba terminada', () => {
    expect(userAccess({ active: true, companyStatus: 'ACTIVE', companyTrialEnded: true })).toBe('TRIAL_ENDED');
  });

  it('empresa suspendida o inactiva', () => {
    expect(userAccess({ active: true, companyStatus: 'SUSPENDED', companyTrialEnded: false })).toBe('SUSPENDED');
    expect(userAccess({ active: true, companyStatus: 'INACTIVE', companyTrialEnded: true })).toBe('INACTIVE');
  });

  it('usuario desactivado: manda sobre todo lo demás', () => {
    expect(userAccess({ active: false, companyStatus: 'ACTIVE', companyTrialEnded: true })).toBe('DISABLED');
  });
});

describe('avatar', () => {
  it('iniciales de una o varias palabras', () => {
    expect(initials('La mejillonera')).toBe('LM');
    expect(initials('Alex')).toBe('AL');
    expect(initials('  ')).toBe('?');
  });

  it('el mismo nombre siempre da el mismo color', () => {
    expect(avatarClass('La mejillonera')).toBe(avatarClass('La mejillonera'));
  });
});

describe('toPagedResponse', () => {
  it('lee los totales de pageable, como los manda la API', () => {
    const r = toPagedResponse({ content: [1, 2], pageable: { page: 1, size: 20, totalPages: 3, totalElements: 45 } });
    expect(r.totalElements).toBe(45);
    expect(r.totalPages).toBe(3);
    expect(r.number).toBe(1);
  });
});
