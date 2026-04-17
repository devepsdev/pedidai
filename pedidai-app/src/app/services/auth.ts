import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';
import { LoginRequest, LoginResponse, RegisterRequest } from '../models/auth.model';
import { UserResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService);
  private router = inject(Router);

  private _authenticated$ = new BehaviorSubject<boolean>(this.hasToken());
  readonly authenticated$ = this._authenticated$.asObservable();

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.api.post<ApiResponse<LoginResponse>>('/auth/login', { email, password } as LoginRequest).pipe(
      map(res => res.data),
      tap(data => {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        this._authenticated$.next(true);
      })
    );
  }

  register(data: RegisterRequest): Observable<ApiResponse<unknown>> {
    return this.api.post<ApiResponse<unknown>>('/companies/register', data);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this._authenticated$.next(false);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getCurrentUser(): UserResponse | null {
    const u = localStorage.getItem('user');
    return u ? JSON.parse(u) : null;
  }

  verifyEmail(token: string): Observable<ApiResponse<unknown>> {
    return this.api.post<ApiResponse<unknown>>('/auth/verify-email', { token });
  }

  forgotPassword(email: string): Observable<ApiResponse<unknown>> {
    return this.api.post<ApiResponse<unknown>>('/auth/forgot-password', { email });
  }

  resetPassword(token: string, password: string): Observable<ApiResponse<unknown>> {
    return this.api.post<ApiResponse<unknown>>('/auth/reset-password', { token, newPassword: password });
  }
}
