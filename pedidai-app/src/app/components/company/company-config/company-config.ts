import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CompanyService } from '../../../services/company';
import { BillingService } from '../../../services/billing';
import { CompanyRequest, MyPlan } from '../../../models/company.model';
import { apiError } from '../../../shared/api-error';

@Component({
  selector: 'app-company-config',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './company-config.html',
})
export class CompanyConfig implements OnInit {
  private translate = inject(TranslateService);
  private fb = inject(FormBuilder);
  private companyService = inject(CompanyService);
  private billing = inject(BillingService);

  /** Plan y suscripción (la página solo la ven administradores). */
  plan = signal<MyPlan | null>(null);
  billingLoading = signal(false);
  billingError = signal('');

  loading = signal(false);
  saving = signal(false);
  error = signal('');
  success = signal(false);
  editMode = signal(false);

  form = this.fb.group({
    name: [{ value: '', disabled: true }],
    taxId: [{ value: '', disabled: true }],
    email: [''],
    phone: [''],
    address: [''],
    city: [''],
    postalCode: [''],
  });

  ngOnInit() {
    this.companyService.getMyPlan().subscribe({ next: plan => this.plan.set(plan), error: () => {} });
    this.loading.set(true);
    this.companyService.get().subscribe({
      next: (company) => {
        this.form.patchValue({
          name: company.name,
          taxId: company.taxId,
          email: company.email ?? '',
          phone: company.phone ?? '',
          address: company.address ?? '',
          city: company.city ?? '',
          postalCode: company.postalCode ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('ERRORS.LOAD_COMPANY'));
        this.loading.set(false);
      },
    });
  }

  startEdit() {
    this.editMode.set(true);
    this.error.set('');
  }

  cancelEdit() {
    this.editMode.set(false);
    this.error.set('');
  }

  save() {
    const raw = this.form.getRawValue();
    const data: CompanyRequest = {
      name: raw.name!,
      taxId: raw.taxId!,
      email: raw.email || undefined,
      phone: raw.phone || undefined,
      address: raw.address || undefined,
      city: raw.city || undefined,
      postalCode: raw.postalCode || undefined,
    };

    this.saving.set(true);
    this.error.set('');
    this.success.set(false);

    this.companyService.update(data).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
        this.editMode.set(false);
        setTimeout(() => this.success.set(false), 3000);
      },
      error: (err) => {
        this.error.set(apiError(err, this.translate.instant('ERRORS.SAVE_CHANGES')));
        this.saving.set(false);
      },
    });
  }

  /** Con suscripción: portal de Stripe (tarjeta, facturas, baja). Sin ella: página de pago. */
  openBilling() {
    const plan = this.plan();
    if (!plan) return;
    this.billingLoading.set(true);
    this.billingError.set('');
    const request = plan.subscriptionStatus ? this.billing.portal() : this.billing.checkout();
    request.subscribe({
      next: url => window.location.href = url,
      error: err => {
        this.billingError.set(apiError(err, this.translate.instant('BILLING.ERROR')));
        this.billingLoading.set(false);
      }
    });
  }
}
