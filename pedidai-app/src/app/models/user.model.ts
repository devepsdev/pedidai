export interface UserResponse {
  uuid: string;
  companyUuid: string;
  companyName: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'USER' | 'SUPER_ADMIN';
  phone?: string;
  isActive: boolean;
  isDeleted: boolean;
  emailVerified: boolean;
  language?: 'es' | 'ca';
  lastLogin?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface UserRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: string;
  password?: string;
}
