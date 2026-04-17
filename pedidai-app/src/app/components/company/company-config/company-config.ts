import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { CompanyService } from '../../../services/company';
import { CompanyRequest } from '../../../models/company.model';

@Component({
  selector: 'app-company-config',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './company-config.html',
})
export class CompanyConfig implements OnInit {
  private fb = inject(FormBuilder);
  private companyService = inject(CompanyService);

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
        this.error.set('Error al cargar los datos de la empresa');
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
      error: () => {
        this.error.set('Error al guardar los cambios');
        this.saving.set(false);
      },
    });
  }
}
