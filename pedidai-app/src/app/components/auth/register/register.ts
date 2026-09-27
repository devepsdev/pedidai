import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../services/auth';
import { AnalyticsService } from '../../../services/analytics.service';

/** Misma regla que el servidor: mínimo 8 caracteres, con alguna letra y algún número. */
export const PASSWORD_PATTERN = /^(?=.*\p{L})(?=.*\d).{8,100}$/u;

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './register.html',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private analytics = inject(AnalyticsService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  saving = signal(false);
  error = signal('');
  showPassword = signal(false);

  form = this.fb.group({
    companyName: ['', [Validators.required, Validators.maxLength(255)]],
    adminFirstName: ['', [Validators.required, Validators.maxLength(100)]],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    acceptTerms: [false, Validators.requiredTrue],
  });

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');

    this.authService.register({
      companyName: raw.companyName!.trim(),
      adminFirstName: raw.adminFirstName!.trim(),
      adminEmail: raw.adminEmail!.trim(),
      adminPassword: raw.adminPassword!,
      acceptTerms: true,
    }).subscribe({
      next: () => {
        // Conversión de la campaña: registro completado (solo se envía si hay consentimiento)
        this.analytics.trackSignUp();
        this.router.navigate(['/dashboard'], { queryParams: { welcome: 1 } });
      },
      error: (err) => {
        this.error.set(err?.error?.message || this.translate.instant('AUTH.REGISTER.ERROR_GENERIC'));
        this.saving.set(false);
      },
    });
  }

  fieldError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }
}
