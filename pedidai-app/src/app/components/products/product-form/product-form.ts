import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ProductService } from '../../../services/product';
import { SupplierService } from '../../../services/supplier';
import { SupplierResponse } from '../../../models/supplier.model';
import { ProductRequest, ProductResponse } from '../../../models/product.model';

const UNITS = ['kg', 'L', 'unitat', 'capsa', 'paquet', 'litre', 'dotzena'];

@Component({
  selector: 'app-product-form',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './product-form.html',
})
export class ProductForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private supplierService = inject(SupplierService);

  loading = signal(false);
  saving = signal(false);
  uploadingImage = signal(false);
  error = signal('');
  uuid = signal<string | null>(null);
  suppliers = signal<SupplierResponse[]>([]);
  currentImageUrl = signal<string | null>(null);
  pendingImageFile = signal<File | null>(null);
  imagePreview = signal<string | null>(null);
  readonly units = UNITS;

  form = this.fb.group({
    name: ['', [Validators.required]],
    category: [''],
    description: [''],
    price: [null as number | null, [Validators.required, Validators.min(0)]],
    volume: [null as number | null],
    unit: [''],
    supplierUuid: ['', [Validators.required]],
  });

  get isEdit() { return !!this.uuid(); }

  ngOnInit() {
    this.supplierService.getAll(0, 200).subscribe({
      next: (data) => this.suppliers.set(data.content),
      error: () => {}
    });

    const id = this.route.snapshot.paramMap.get('uuid');
    if (id) {
      this.uuid.set(id);
      this.loading.set(true);
      this.productService.getByUuid(id).subscribe({
        next: (p) => {
          this.form.patchValue({
            name: p.name,
            category: p.category ?? '',
            description: p.description ?? '',
            price: p.price,
            volume: p.volume ?? null,
            unit: p.unit ?? '',
            supplierUuid: p.supplier?.uuid ?? '',
          });
          if (p.imageUrl) this.currentImageUrl.set(p.imageUrl);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Error al cargar el producto');
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
    const data: ProductRequest = {
      name: raw.name!,
      category: raw.category || undefined,
      description: raw.description || undefined,
      price: raw.price!,
      volume: raw.volume ?? undefined,
      unit: raw.unit || undefined,
      supplierUuid: raw.supplierUuid!,
    };

    this.saving.set(true);
    this.error.set('');

    const obs = this.isEdit
      ? this.productService.update(this.uuid()!, data)
      : this.productService.create(data);

    obs.subscribe({
      next: (product: ProductResponse) => {
        this.saving.set(false);
        const file = this.pendingImageFile();
        if (file) {
          this.uploadingImage.set(true);
          this.productService.uploadImage(product.uuid, file).subscribe({
            next: () => {
              this.uploadingImage.set(false);
              this.router.navigate(['/products']);
            },
            error: () => {
              this.error.set('Error al subir la imagen');
              this.uploadingImage.set(false);
            }
          });
        } else {
          this.router.navigate(['/products']);
        }
      },
      error: () => {
        this.error.set('Error al guardar el producto');
        this.saving.set(false);
      }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.pendingImageFile.set(file);
    const reader = new FileReader();
    reader.onload = (e) => this.imagePreview.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  cancel() {
    this.router.navigate(['/products']);
  }

  fieldError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }
}
