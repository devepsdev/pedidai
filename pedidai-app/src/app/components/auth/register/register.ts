import { Component, inject, signal } from '@angular/core';
import { AbstractControl, ReactiveFormsModule, FormBuilder, Validators, ValidatorFn } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../services/auth';

const passwordStrength: ValidatorFn = (ctrl: AbstractControl) => {
  const v = ctrl.value as string;
  if (!v) return null;
  if (!/[A-Z]/.test(v)) return { uppercase: true };
  if (!/[0-9]/.test(v)) return { number: true };
  return null;
};

const passwordMatch: ValidatorFn = (group: AbstractControl) => {
  const pw = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { mismatch: true } : null;
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './register.html',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  saving = signal(false);
  error = signal('');
  success = signal(false);

  form = this.fb.group({
    companyName: ['', Validators.required],
    taxId: ['', Validators.required],
    companyEmail: ['', [Validators.required, Validators.email]],
    companyPhone: [''],
    companyAddress: [''],
    companyCity: [''],
    companyPostalCode: [''],
    adminFirstName: ['', Validators.required],
    adminLastName: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8), passwordStrength]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatch });

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');

    this.authService.register({
      companyName: raw.companyName!,
      taxId: raw.taxId!,
      companyEmail: raw.companyEmail!,
      companyPhone: raw.companyPhone || undefined,
      companyAddress: raw.companyAddress || undefined,
      companyCity: raw.companyCity || undefined,
      companyPostalCode: raw.companyPostalCode || undefined,
      adminEmail: raw.companyEmail!,
      adminPassword: raw.password!,
      adminFirstName: raw.adminFirstName!,
      adminLastName: raw.adminLastName!,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
      },
      error: (err) => {
        const msg = err?.error?.message;
        this.error.set(msg || 'Error al crear la cuenta. Inténtalo de nuevo.');
        this.saving.set(false);
      },
    });
  }

  fieldError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  get passwordErrors() {
    const ctrl = this.form.get('password');
    if (!ctrl?.touched || !ctrl?.errors) return null;
    return ctrl.errors;
  }

  get mismatch() {
    return this.form.errors?.['mismatch'] && this.form.get('confirmPassword')?.touched;
  }
}
