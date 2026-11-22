import { companyBadge, companyBadgeClass, userAccess } from './company-badge';

describe('companyBadge', () => {
  it('prueba terminada: ACTIVE con la prueba acabada', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: '2026-11-05T11:14:25', trialEnded: true })).toBe('TRIAL_ENDED');
  });

  it('en prueba: ACTIVE con la prueba en curso', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: '2099-01-01T00:00:00', trialEnded: false })).toBe('TRIAL');
  });

  it('de pago o plataforma: ACTIVE sin fecha de prueba', () => {
    expect(companyBadge({ status: 'ACTIVE', trialEndsAt: null, trialEnded: false })).toBe('ACTIVE');
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
