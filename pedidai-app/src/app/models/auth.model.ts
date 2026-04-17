import { UserResponse } from './user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  user: UserResponse;
}

export interface RegisterRequest {
  companyName: string;
  taxId: string;
  companyEmail: string;
  companyPhone?: string;
  companyAddress?: string;
  companyCity?: string;
  companyPostalCode?: string;
  adminEmail: string;
  adminPassword: string;
  adminFirstName: string;
  adminLastName: string;
  adminPhone?: string;
}
