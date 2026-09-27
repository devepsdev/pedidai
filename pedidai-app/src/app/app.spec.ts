import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { App } from './app';
import { jwtInterceptor } from './interceptors/jwt-interceptor';
import { PASSWORD_PATTERN } from './components/auth/register/register';
import { formatMoney } from './shared/format';
import { environment } from '../environments/environment';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        provideTranslateService({ fallbackLang: 'es' }),
      ],
    }).compileComponents();
  });

  it('arranca la aplicación', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('usa castellano o catalán como idioma de la página', () => {
    TestBed.createComponent(App);
    expect(['es', 'ca']).toContain(document.documentElement.lang);
  });
});

describe('Regla de contraseña del registro (igual que el servidor)', () => {
  it('exige 8 caracteres con alguna letra y algún número', () => {
    expect(PASSWORD_PATTERN.test('restaurante1')).toBe(true);
    expect(PASSWORD_PATTERN.test('Bar12345')).toBe(true);
    expect(PASSWORD_PATTERN.test('abcdefgh')).toBe(false);
    expect(PASSWORD_PATTERN.test('12345678')).toBe(false);
    expect(PASSWORD_PATTERN.test('abc1')).toBe(false);
  });
});

describe('Interceptor de sesión e idioma', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
      ],
    });
  });

  it('añade el token y el idioma a la API y al asistente, pero no a terceros', () => {
    localStorage.setItem('token', 'tok');
    localStorage.setItem('lang', 'ca');
    const http = TestBed.inject(HttpClient);
    const ctrl = TestBed.inject(HttpTestingController);

    http.get(environment.apiUrl + '/users/me').subscribe();
    http.post(environment.aiUrl + '/process-order', {}).subscribe();
    http.get('https://terceros.example/recurso').subscribe();

    const api = ctrl.expectOne(environment.apiUrl + '/users/me');
    expect(api.request.headers.get('Authorization')).toBe('Bearer tok');
    expect(api.request.headers.get('Accept-Language')).toBe('ca');
    const ai = ctrl.expectOne(environment.aiUrl + '/process-order');
    expect(ai.request.headers.get('Authorization')).toBe('Bearer tok');
    const external = ctrl.expectOne('https://terceros.example/recurso');
    expect(external.request.headers.has('Authorization')).toBe(false);
  });
});

describe('Formato de importes', () => {
  it('usa el formato de cada idioma', () => {
    expect(formatMoney(1234.5, 'es')).toContain('€');
    expect(formatMoney(10.4, 'ca')).toMatch(/10,40/);
    expect(formatMoney(null, 'es')).toBe('—');
  });
});
