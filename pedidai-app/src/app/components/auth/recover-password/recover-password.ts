import { Component, signal, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-recover-password',
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './recover-password.html',
  styleUrl: './recover-password.scss',
})
export class RecoverPassword {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private translate = inject(TranslateService);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  loading = signal(false);
  success = signal(false);
  error = signal('');

  get email() { return this.form.get('email'); }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.forgotPassword(this.email!.value!).subscribe({
      next: () => { this.loading.set(false); this.success.set(true); },
      error: () => { this.loading.set(false); this.error.set(this.translate.instant('ERRORS.RECOVER_ERROR')); },
    });
  }
}
