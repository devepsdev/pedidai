import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';
import { PASSWORD_PATTERN } from '../auth/register/register';
import { apiError } from '../../shared/api-error';

/** Mi cuenta: datos del usuario y cambio de contraseña (para cualquier rol). */
@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './account.html',
})
export class Account {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private userService = inject(UserService);
  private translate = inject(TranslateService);

  user = this.auth.getCurrentUser();
  saving = signal(false);
  error = signal('');
  success = signal(false);

  form = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', Validators.required],
  }, { validators: (g: AbstractControl): ValidationErrors | null =>
      g.get('newPassword')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true } });

  get mismatch(): boolean {
    return this.form.hasError('mismatch') && !!this.form.get('confirmPassword')?.touched;
  }

  roleLabel(): string {
    const role = this.user?.role;
    return role === 'ADMIN' ? this.translate.instant('USERS.FORM.ROLE_ADMIN')
      : role === 'SUPER_ADMIN' ? 'SUPER_ADMIN'
      : this.translate.instant('USERS.FORM.ROLE_USER');
  }

  changePassword() {
    if (!this.user) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    this.success.set(false);
    this.userService.changePassword(this.user.uuid, currentPassword!, newPassword!).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
        this.form.reset();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(apiError(err, this.translate.instant('ACCOUNT.ERROR')));
      },
    });
  }
}
