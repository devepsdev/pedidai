import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../services/auth';
import { BillingService } from '../../../services/billing';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private translate = inject(TranslateService);
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private billing = inject(BillingService);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  loading = signal(false);
  errorMsg = signal('');
  /** La prueba o la suscripción ha terminado: se ofrece contratar sin iniciar sesión. */
  trialEnded = signal(false);
  /** Ya fue cliente de pago (se dio de baja o dejó de pagar), no viene de la prueba gratuita. */
  subscriptionEnded = signal(false);
  billingEnabled = signal(false);
  checkoutLoading = signal(false);
  /** Vuelta de la página de pago de Stripe. */
  paid = signal(this.route.snapshot.queryParamMap.get('pago') === 'ok');

  ngOnInit() {
    this.billing.isEnabled().subscribe({ next: enabled => this.billingEnabled.set(enabled), error: () => {} });
  }

  get email() { return this.form.get('email'); }
  get password() { return this.form.get('password'); }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');
    this.trialEnded.set(false);
    this.subscriptionEnded.set(false);
    const { email, password } = this.form.value;
    this.auth.login(email!, password!).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.errorMsg.set(err.error?.message || this.translate.instant('ERRORS.LOGIN_FAILED'));
        const code = err.error?.code;
        this.subscriptionEnded.set(code === 'error.auth.subscriptionEnded');
        this.trialEnded.set(code === 'error.auth.trialEnded' || this.subscriptionEnded());
        this.loading.set(false);
      }
    });
  }

  /** Prueba terminada: con el mismo email y contraseña, va a la página de pago de Stripe. */
  checkoutAfterTrial() {
    const { email, password } = this.form.value;
    this.checkoutLoading.set(true);
    this.billing.checkoutAfterTrial(email!, password!).subscribe({
      next: url => window.location.href = url,
      error: (err) => {
        this.errorMsg.set(err.error?.message || this.translate.instant('BILLING.ERROR'));
        this.trialEnded.set(false);
        this.checkoutLoading.set(false);
      }
    });
  }
}
