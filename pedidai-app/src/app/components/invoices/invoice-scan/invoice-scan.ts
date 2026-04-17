import { Component, inject, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { InvoiceService } from '../../../services/invoice';
import { SupplierService } from '../../../services/supplier';
import {
  InvoiceConfirmRequestDTO,
  InvoiceProductConfirmDTO,
  InvoiceScanResultDTO,
} from '../../../models/invoice.model';
import { SupplierResponse } from '../../../models/supplier.model';

@Component({
  selector: 'app-invoice-scan',
  imports: [DecimalPipe, TranslateModule],
  templateUrl: './invoice-scan.html',
})
export class InvoiceScan implements OnInit, OnDestroy {
  private invoiceService = inject(InvoiceService);
  private supplierService = inject(SupplierService);
  private sanitizer = inject(DomSanitizer);

  step = signal<1 | 2 | 3>(1);
  selectedFile = signal<File | null>(null);
  previewUrl = signal<SafeUrl | null>(null);
  isPdf = signal(false);
  isDragOver = signal(false);

  suppliers = signal<SupplierResponse[]>([]);
  selectedSupplierUuid = signal('');

  scanResult = signal<InvoiceScanResultDTO | null>(null);
  checkedProducts = signal<boolean[]>([]);
  manualSupplierUuid = signal('');

  loading = signal(false);
  error = signal('');

  confirmResult = signal<{ created: number; updated: number; skipped: number } | null>(null);

  private objectUrl: string | null = null;

  selectedCount = computed(() => this.checkedProducts().filter(Boolean).length);
  totalCount = computed(() => this.checkedProducts().length);
  allChecked = computed(() => this.totalCount() > 0 && this.selectedCount() === this.totalCount());

  effectiveSupplierUuid = computed(() => {
    const r = this.scanResult();
    return r?.matchedSupplierUuid ?? this.manualSupplierUuid();
  });

  ngOnInit() {
    this.supplierService.getAll(0, 200, 'name', 'asc').subscribe({
      next: d => this.suppliers.set(d.content),
      error: () => {},
    });
  }

  ngOnDestroy() {
    this.revokeObjectUrl();
  }

  private revokeObjectUrl() {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }

  onDragOver(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    this.isDragOver.set(true);
  }

  onDragLeave(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    this.isDragOver.set(false);
  }

  onDrop(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    this.isDragOver.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) this.processFile(file);
  }

  onFileSelected(e: Event) {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) this.processFile(file);
    (e.target as HTMLInputElement).value = '';
  }

  private processFile(file: File) {
    const allowed = ['image/jpeg', 'image/png', 'image/bmp', 'application/pdf'];
    if (!allowed.includes(file.type)) {
      this.error.set('Format no permès. Utilitza JPEG, PNG, BMP o PDF.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.error.set('El fitxer supera els 10 MB màxims permesos.');
      return;
    }
    this.error.set('');
    this.revokeObjectUrl();
    this.selectedFile.set(file);
    this.isPdf.set(file.type === 'application/pdf');
    this.objectUrl = URL.createObjectURL(file);
    this.previewUrl.set(this.sanitizer.bypassSecurityTrustUrl(this.objectUrl));
  }

  scan() {
    const file = this.selectedFile();
    if (!file || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    const supp = this.selectedSupplierUuid() || undefined;
    this.invoiceService.scanInvoice(file, supp).subscribe({
      next: resp => {
        const data = resp.data;
        this.scanResult.set(data);
        this.checkedProducts.set(data.products.map(() => true));
        this.manualSupplierUuid.set(data.matchedSupplierUuid ?? '');
        this.loading.set(false);
        this.step.set(2);
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error en escanejar la factura. Torna-ho a intentar.');
        this.loading.set(false);
      },
    });
  }

  toggleProduct(i: number) {
    const arr = [...this.checkedProducts()];
    arr[i] = !arr[i];
    this.checkedProducts.set(arr);
  }

  toggleAll() {
    const all = this.allChecked();
    this.checkedProducts.set(this.checkedProducts().map(() => !all));
  }

  confirm() {
    const result = this.scanResult();
    const suppUuid = this.effectiveSupplierUuid();
    if (!result) return;
    if (!suppUuid) {
      this.error.set('Selecciona un proveïdor per continuar.');
      return;
    }
    const products: InvoiceProductConfirmDTO[] = result.products
      .filter((_, i) => this.checkedProducts()[i])
      .map(p => ({
        name: p.name,
        matchedProductUuid: p.matchedProductUuid,
        unitPrice: p.unitPrice,
        action: p.action,
      }));
    if (!products.length) {
      this.error.set('Selecciona almenys un producte per confirmar.');
      return;
    }
    const request: InvoiceConfirmRequestDTO = { supplierUuid: suppUuid, products };
    this.loading.set(true);
    this.error.set('');
    this.invoiceService.confirmInvoice(request).subscribe({
      next: () => {
        const sel = result.products.filter((_, i) => this.checkedProducts()[i]);
        this.confirmResult.set({
          created: sel.filter(p => p.action === 'CREATED').length,
          updated: sel.filter(p => p.action === 'UPDATED').length,
          skipped: sel.filter(p => p.action === 'SKIPPED').length,
        });
        this.loading.set(false);
        this.step.set(3);
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error en confirmar la importació. Torna-ho a intentar.');
        this.loading.set(false);
      },
    });
  }

  reset() {
    this.step.set(1);
    this.selectedFile.set(null);
    this.revokeObjectUrl();
    this.previewUrl.set(null);
    this.isPdf.set(false);
    this.selectedSupplierUuid.set('');
    this.scanResult.set(null);
    this.checkedProducts.set([]);
    this.manualSupplierUuid.set('');
    this.error.set('');
    this.confirmResult.set(null);
  }

  actionBadgeClass(action: string): string {
    const base = 'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ';
    if (action === 'CREATED') return base + 'bg-emerald-100 text-emerald-700';
    if (action === 'UPDATED') return base + 'bg-orange-100 text-orange-700';
    return base + 'bg-slate-100 text-slate-500';
  }

  formatFileSize(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(2) + ' MB';
  }
}
