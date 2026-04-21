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
  trialEndsAt: string | null;
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
