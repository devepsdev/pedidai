export type CompanyStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED';

export interface CompanyResponse {
  uuid: string;
  name: string;
  taxId: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  status: CompanyStatus;
  createdAt: string;
  updatedAt?: string;
  trialEndsAt?: string | null;
}

export interface MyPlan {
  status: string;
  /** Sin valor (la API omite los nulos) = cliente de pago. */
  trialEndsAt?: string | null;
  /** Pago en línea con Stripe configurado; si no, se ofrece el contacto por email. */
  billingEnabled: boolean;
  /** Estado de la suscripción en Stripe ('active', 'trialing', 'past_due', 'canceled'…); null si no hay. */
  subscriptionStatus?: string | null;
}

export interface CompanyRequest {
  name: string;
  taxId: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  postalCode?: string;
}
