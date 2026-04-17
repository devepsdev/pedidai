import { Component, signal, inject, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../services/auth';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const pw = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);

  token = signal('');
  loading = signal(false);
  success = signal(false);
  error = signal('');

  readonly pwPattern = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9\s]).{8,}$/;

  form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(this.pwPattern)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordsMatch });

  get pw() { return this.form.get('password'); }
  get mismatch() { return this.form.hasError('mismatch') && this.form.get('confirmPassword')?.touched; }

  ngOnInit() {
    const t = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!t) this.error.set('Enllaç invàlid o caducat.');
    this.token.set(t);
  }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (!this.token()) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.resetPassword(this.token(), this.pw!.value!).subscribe({
      next: () => { this.loading.set(false); this.success.set(true); },
      error: () => { this.loading.set(false); this.error.set('Enllaç invàlid o caducat. Sol·licita un nou correu de recuperació.'); },
    });
  }
}
