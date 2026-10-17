import { Component, inject, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { InvoiceService } from '../../../services/invoice';
import { SupplierService } from '../../../services/supplier';
import { LanguageService } from '../../../services/language.service';
import { DocumentKind, InvoiceConfirmRequestDTO, InvoiceLineStatus, InvoiceScanResultDTO } from '../../../models/invoice.model';
import { SupplierResponse } from '../../../models/supplier.model';
import { formatMoney } from '../../../shared/format';

/** Línea del albarán que el usuario puede revisar y corregir antes de guardarla. */
interface EditableLine {
  selected: boolean;
  name: string;
  genericName: string;
  quantity: number | null;
  unit: string;
  unitPrice: number | null;
  status: InvoiceLineStatus;
  matchedProductUuid?: string;
  previousPrice?: number;
}

const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
/** Las tarifas también pueden ser hojas de cálculo; el navegador no siempre da su tipo, así que va por extensión. */
const SHEET_EXTENSIONS = ['.xlsx', '.xls', '.csv'];
/** Tipos que acepta el selector de archivos. */
const ACCEPT = [...ACCEPTED_TYPES, ...SHEET_EXTENSIONS].join(',');
const MAX_BYTES = 10 * 1024 * 1024;

@Component({
  selector: 'app-invoice-scan',
  imports: [RouterLink, TranslateModule],
  templateUrl: './invoice-scan.html',
})
export class InvoiceScan implements OnInit, OnDestroy {
  private invoiceService = inject(InvoiceService);
  private supplierService = inject(SupplierService);
  private sanitizer = inject(DomSanitizer);
  private translate = inject(TranslateService);
  private language = inject(LanguageService);

  readonly accept = ACCEPT;
  /**
   * Albarán o factura (precios pagados) o tarifa o presupuesto (precios ofrecidos). Lo detecta la IA al leer
   * el documento y el usuario lo puede cambiar en la revisión.
   */
  readonly kinds: DocumentKind[] = ['INVOICE', 'PRICE_LIST'];
  kind = signal<DocumentKind>('INVOICE');
  isPriceList = computed(() => this.kind() === 'PRICE_LIST');
  /** Sufijo de las claves de texto que cambian según el tipo de documento. */
  k = computed(() => (this.isPriceList() ? '_PRICE_LIST' : ''));

  step = signal<1 | 2 | 3>(1);
  selectedFile = signal<File | null>(null);
  previewUrl = signal<SafeUrl | null>(null);
  isPdf = signal(false);
  /** Extensión en mayúsculas si es una hoja de cálculo (XLSX, XLS, CSV). */
  sheetType = signal('');
  isDragOver = signal(false);

  suppliers = signal<SupplierResponse[]>([]);
  preselectedSupplierUuid = signal('');

  scanResult = signal<InvoiceScanResultDTO | null>(null);
  lines = signal<EditableLine[]>([]);
  /** '' = crear un proveedor nuevo con los datos detectados. */
  supplierUuid = signal('');
  newSupplierName = signal('');
  newSupplierEmail = signal('');
  invoiceDate = signal('');
  invoiceNumber = signal('');
  validUntil = signal('');

  loading = signal(false);
  error = signal('');
  confirmResult = signal<InvoiceScanResultDTO | null>(null);

  private objectUrl: string | null = null;

  selectedCount = computed(() => this.lines().filter(l => l.selected).length);
  allSelected = computed(() => this.lines().length > 0 && this.lines().every(l => l.selected));
  increases = computed(() => this.lines().filter(l => l.selected && this.change(l) !== null && this.change(l)! > 0).length);

  ngOnInit() {
    this.loadSuppliers();
  }

  /** Corrige en la revisión el tipo de documento que ha detectado la IA. */
  setKind(kind: DocumentKind) {
    this.kind.set(kind);
  }

  ngOnDestroy() {
    this.revokeObjectUrl();
  }

  private loadSuppliers() {
    this.supplierService.getAll(0, 200, 'name', 'asc').subscribe({
      next: d => this.suppliers.set(d.content),
      error: () => { /* el selector queda vacío: se puede crear el proveedor */ },
    });
  }

  // ───────── Paso 1: archivo ─────────

  onDragOver(e: DragEvent) {
    e.preventDefault();
    this.isDragOver.set(true);
  }

  onDragLeave(e: DragEvent) {
    e.preventDefault();
    this.isDragOver.set(false);
  }

  onDrop(e: DragEvent) {
    e.preventDefault();
    this.isDragOver.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) this.processFile(file);
  }

  onFileSelected(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.processFile(file);
    input.value = '';
  }

  private processFile(file: File) {
    const name = file.name.toLowerCase();
    const sheet = SHEET_EXTENSIONS.find(ext => name.endsWith(ext));
    if (!sheet && !ACCEPTED_TYPES.includes(file.type)) {
      this.error.set(this.translate.instant('INVOICE_SCAN.ERR_FORMAT'));
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set(this.translate.instant('INVOICE_SCAN.ERR_SIZE'));
      return;
    }
    this.error.set('');
    this.revokeObjectUrl();
    this.selectedFile.set(file);
    this.isPdf.set(!sheet && file.type === 'application/pdf');
    this.sheetType.set(sheet ? sheet.slice(1).toUpperCase() : '');
    this.objectUrl = URL.createObjectURL(file);
    this.previewUrl.set(this.sanitizer.bypassSecurityTrustUrl(this.objectUrl));
  }

  scan() {
    const file = this.selectedFile();
    if (!file || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.invoiceService.scanInvoice(file, this.preselectedSupplierUuid() || undefined, 'AUTO').subscribe({
      next: resp => {
        const data = resp.data;
        this.scanResult.set(data);
        this.kind.set(data.documentKind === 'PRICE_LIST' ? 'PRICE_LIST' : 'INVOICE');
        this.lines.set(data.products.map(p => ({
          selected: p.unitPrice != null,
          name: p.name,
          genericName: p.genericName ?? '',
          quantity: p.quantity ?? null,
          unit: p.unit ?? '',
          unitPrice: p.unitPrice ?? null,
          status: p.status,
          matchedProductUuid: p.matchedProductUuid,
          previousPrice: p.previousPrice,
        })));
        this.supplierUuid.set(data.matchedSupplierUuid ?? '');
        this.newSupplierName.set(data.detectedSupplierName ?? '');
        this.newSupplierEmail.set('');
        this.invoiceDate.set(data.invoiceDate ?? new Date().toISOString().slice(0, 10));
        this.invoiceNumber.set(data.invoiceNumber ?? '');
        this.validUntil.set(data.validUntil ?? '');
        this.loading.set(false);
        this.step.set(2);
      },
      error: err => {
        this.error.set(err?.error?.message ?? this.translate.instant('INVOICE_SCAN.ERR_SCAN'));
        this.loading.set(false);
      },
    });
  }

  // ───────── Paso 2: revisión ─────────

  updateLine(i: number, patch: Partial<EditableLine>) {
    this.lines.update(ls => ls.map((l, idx) => (idx === i ? { ...l, ...patch } : l)));
  }

  toggleAll() {
    const all = this.allSelected();
    this.lines.update(ls => ls.map(l => ({ ...l, selected: !all })));
  }

  /** Variación del precio escrito respecto al último conocido, en %. */
  change(line: EditableLine): number | null {
    if (line.previousPrice == null || line.unitPrice == null || line.previousPrice === 0) return null;
    const pct = ((line.unitPrice - line.previousPrice) / line.previousPrice) * 100;
    return Math.abs(pct) < 0.05 ? 0 : pct;
  }

  confirm() {
    const selected = this.lines().filter(l => l.selected);
    if (!selected.length) {
      this.error.set(this.translate.instant('INVOICE_SCAN.ERR_NO_LINES'));
      return;
    }
    if (selected.some(l => l.unitPrice == null || l.unitPrice < 0 || !l.name.trim())) {
      this.error.set(this.translate.instant('INVOICE_SCAN.ERR_MISSING_PRICE'));
      return;
    }
    const creatingSupplier = !this.supplierUuid();
    if (creatingSupplier && !this.newSupplierName().trim()) {
      this.error.set(this.translate.instant('INVOICE_SCAN.ERR_SUPPLIER'));
      return;
    }

    const request: InvoiceConfirmRequestDTO = {
      ...(creatingSupplier
        ? { newSupplier: { name: this.newSupplierName().trim(), email: this.newSupplierEmail().trim() || undefined,
            phone: this.scanResult()?.detectedSupplierPhone } }
        : { supplierUuid: this.supplierUuid() }),
      invoiceNumber: this.invoiceNumber().trim() || undefined,
      invoiceDate: this.invoiceDate() || undefined,
      documentKind: this.kind(),
      validUntil: this.isPriceList() ? this.validUntil() || undefined : undefined,
      products: selected.map(l => ({
        name: l.name.trim(),
        genericName: l.genericName.trim() || undefined,
        quantity: this.isPriceList() ? undefined : l.quantity ?? undefined,
        unit: l.unit.trim() || undefined,
        unitPrice: l.unitPrice!,
        matchedProductUuid: creatingSupplier ? undefined : l.matchedProductUuid,
      })),
    };

    this.loading.set(true);
    this.error.set('');
    this.invoiceService.confirmInvoice(request).subscribe({
      next: resp => {
        this.confirmResult.set(resp.data);
        this.loading.set(false);
        this.step.set(3);
        this.loadSuppliers();
      },
      error: err => {
        this.error.set(err?.error?.message ?? this.translate.instant('INVOICE_SCAN.ERR_CONFIRM'));
        this.loading.set(false);
      },
    });
  }

  reset() {
    this.step.set(1);
    this.clearFile();
    this.kind.set('INVOICE');
    this.preselectedSupplierUuid.set('');
    this.validUntil.set('');
    this.scanResult.set(null);
    this.lines.set([]);
    this.error.set('');
    this.confirmResult.set(null);
  }

  private clearFile() {
    this.selectedFile.set(null);
    this.revokeObjectUrl();
    this.previewUrl.set(null);
    this.isPdf.set(false);
    this.sheetType.set('');
  }

  /** Texto con plural: la clave con sufijo _ONE para 1 (y _ZERO para 0, si existe) y la clave tal cual para el resto. */
  plural(key: string, count: number, params: Record<string, unknown> = {}): string {
    const suffix = count === 1 ? '_ONE' : count === 0 ? '_ZERO' : '';
    const fullKey = key + suffix;
    const value = { count, ...params };
    const text = this.translate.instant(fullKey, value);
    return suffix && text === fullKey ? this.translate.instant(key, value) : text;
  }

  /** «Hemos guardado 5 precios (1 producto nuevo).» */
  doneText(r: InvoiceScanResultDTO): string {
    const prices = this.plural(this.isPriceList() ? 'INVOICE_SCAN.PRICES_OFFERED' : 'INVOICE_SCAN.PRICES_SAVED', r.pricesRecorded ?? 0);
    const products = this.plural('INVOICE_SCAN.NEW_PRODUCTS', r.productsCreated ?? 0);
    return this.translate.instant('INVOICE_SCAN.DONE_TEXT' + this.k(), { prices, products });
  }

  money(v: number | null | undefined) {
    return formatMoney(v, this.language.current());
  }

  formatFileSize(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  }

  private revokeObjectUrl() {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
