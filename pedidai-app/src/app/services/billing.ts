import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';

/** Pago de la suscripción con Stripe: la API devuelve la dirección de Stripe y el navegador va allí. */
@Injectable({ providedIn: 'root' })
export class BillingService {
  private api = inject(ApiService);

  /** El pago en línea está configurado (sin claves de Stripe, la web ofrece el contacto por email). */
  isEnabled(): Observable<boolean> {
    return this.api.get<ApiResponse<boolean>>('/billing/enabled').pipe(map(r => r.data));
  }

  /** Página de pago para la empresa del administrador conectado. */
  checkout(): Observable<string> {
    return this.api.post<ApiResponse<{ url: string }>>('/billing/checkout', {}).pipe(map(r => r.data.url));
  }

  /** Página de pago cuando la prueba ya terminó y no se puede iniciar sesión. */
  checkoutAfterTrial(email: string, password: string): Observable<string> {
    return this.api.post<ApiResponse<{ url: string }>>('/billing/checkout-after-trial', { email, password })
      .pipe(map(r => r.data.url));
  }

  /** Portal del cliente de Stripe: tarjeta, facturas y baja. */
  portal(): Observable<string> {
    return this.api.post<ApiResponse<{ url: string }>>('/billing/portal', {}).pipe(map(r => r.data.url));
  }
}
