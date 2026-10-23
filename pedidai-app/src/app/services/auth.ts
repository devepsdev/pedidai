import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';
import { LoginRequest, LoginResponse, RegisterRequest } from '../models/auth.model';
import { UserResponse } from '../models/user.model';
import { LanguageService } from './language.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService);
  private router = inject(Router);
  private language = inject(LanguageService);

  /** A partir de un día se pide un token nuevo: la sesión dura 30 días desde la última renovación. */
  private static readonly RENEW_AFTER_MS = 24 * 60 * 60 * 1000;
  private renewing = false;

  private _authenticated$ = new BehaviorSubject<boolean>(this.hasToken());
  readonly authenticated$ = this._authenticated$.asObservable();

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.api.post<ApiResponse<LoginResponse>>('/auth/login', { email, password } as LoginRequest).pipe(
      map(res => res.data),
      tap(data => this.startSession(data))
    );
  }

  /** Crea la cuenta y deja la sesión iniciada (la prueba empieza en ese momento). */
  register(data: RegisterRequest): Observable<LoginResponse> {
    return this.api.post<ApiResponse<LoginResponse>>('/companies/register', data).pipe(
      map(res => res.data),
      tap(session => this.startSession(session))
    );
  }

  /** Guarda la sesión (login, alta, renovación o cambio de contraseña, que da un token nuevo). */
  startSession(data: LoginResponse): void {
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    this.language.adoptFromAccount(data.user?.language);
    this._authenticated$.next(true);
  }

  /**
   * Mantiene la sesión abierta mientras se usa la app: si el token tiene más de un día (o es de los
   * antiguos de una hora, sin versión), pide uno nuevo. Se llama al arrancar y al volver a la pestaña.
   */
  renewIfNeeded(): void {
    const token = this.getToken();
    if (!token || this.renewing) return;
    const payload = this.payload(token);
    if (!payload) return;
    const old = typeof payload['iat'] === 'number' && Date.now() - payload['iat'] * 1000 > AuthService.RENEW_AFTER_MS;
    if (!old && payload['tv'] !== undefined) return;
    this.renewing = true;
    this.api.post<ApiResponse<LoginResponse>>('/auth/refresh', {}).subscribe({
      next: res => {
        this.startSession(res.data);
        this.renewing = false;
      },
      // Si el token ya no vale, el errorInterceptor cierra la sesión (401)
      error: () => (this.renewing = false),
    });
  }

  private payload(token: string): Record<string, unknown> | null {
    try {
      return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    } catch {
      return null;
    }
  }

  /** Datos actualizados del usuario (p. ej. si ya ha verificado el email). */
  refreshMe(): Observable<UserResponse> {
    return this.api.get<ApiResponse<UserResponse>>('/users/me').pipe(
      map(res => res.data),
      tap(user => localStorage.setItem('user', JSON.stringify(user)))
    );
  }

  resendMyVerification(): Observable<ApiResponse<unknown>> {
    return this.api.post<ApiResponse<unknown>>('/users/me/resend-verification', {});
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
