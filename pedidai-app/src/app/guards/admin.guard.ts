import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

/** Gestión (informes, datos de la empresa y usuarios): solo el administrador de la empresa. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.getCurrentUser()?.role === 'ADMIN') return true;
  router.navigate(['/dashboard']);
  return false;
};
