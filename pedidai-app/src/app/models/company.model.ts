export type CompanyStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

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
