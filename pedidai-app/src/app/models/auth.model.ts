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

/** Alta con lo mínimo para empezar la prueba; el resto de datos se completan después. */
export interface RegisterRequest {
  companyName: string;
  adminFirstName: string;
  adminEmail: string;
  adminPassword: string;
  acceptTerms: boolean;
}
