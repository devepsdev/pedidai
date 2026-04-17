import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SupplierService } from '../../../services/supplier';
import { SupplierRequest } from '../../../models/supplier.model';

@Component({
  selector: 'app-supplier-form',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './supplier-form.html',
})
export class SupplierForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private supplierService = inject(SupplierService);

  loading = signal(false);
  saving = signal(false);
  error = signal('');
  uuid = signal<string | null>(null);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    contactName: [''],
    email: ['', [Validators.email]],
    phone: [''],
    address: [''],
    notes: [''],
  });

  get isEdit() { return !!this.uuid(); }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('uuid');
    if (id) {
      this.uuid.set(id);
      this.loading.set(true);
      this.supplierService.getByUuid(id).subscribe({
        next: (s) => {
          this.form.patchValue({
            name: s.name,
            contactName: s.contactName ?? '',
            email: s.email ?? '',
            phone: s.phone ?? '',
            address: s.address ?? '',
            notes: s.notes ?? '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Error al cargar el proveedor');
          this.loading.set(false);
        }
      });
    }
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const data: SupplierRequest = {
      name: raw.name!,
      contactName: raw.contactName || undefined,
      email: raw.email || undefined,
      phone: raw.phone || undefined,
      address: raw.address || undefined,
      notes: raw.notes || undefined,
    };

    this.saving.set(true);
    this.error.set('');

    const obs = this.isEdit
      ? this.supplierService.update(this.uuid()!, data)
      : this.supplierService.create(data);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/suppliers']);
      },
      error: () => {
        this.error.set('Error al guardar el proveedor');
        this.saving.set(false);
      }
    });
  }

  cancel() {
    this.router.navigate(['/suppliers']);
  }

  fieldError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }
}
