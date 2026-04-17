import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SupplierService } from '../../../services/supplier';
import { SupplierResponse } from '../../../models/supplier.model';

@Component({
  selector: 'app-supplier-list',
  imports: [FormsModule, TranslateModule],
  templateUrl: './supplier-list.html',
})
export class SupplierList implements OnInit {
  private supplierService = inject(SupplierService);
  private router = inject(Router);

  suppliers = signal<SupplierResponse[]>([]);
  loading = signal(false);
  error = signal('');
  searchText = signal('');
  currentPage = signal(0);
  pageSize = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);

  // Toggle modal
  showToggleModal = signal(false);
  pendingSupplier = signal<SupplierResponse | null>(null);

  // Advanced search
  showAdvanced = signal(false);
  advName = signal('');
  advContactName = signal('');
  advEmail = signal('');
  advPhone = signal('');

  readonly pageSizeOptions = [5, 10, 25, 50];

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');

    if (this.showAdvanced()) {
      const params: Record<string, string | number | undefined> = {
        page: this.currentPage(),
        size: this.pageSize(),
        sortBy: 'name',
        sortDir: 'asc',
      };
      if (this.advName().trim()) params['name'] = this.advName().trim();
      if (this.advContactName().trim()) params['contactName'] = this.advContactName().trim();
      if (this.advEmail().trim()) params['email'] = this.advEmail().trim();
      if (this.advPhone().trim()) params['phone'] = this.advPhone().trim();

      this.supplierService.filter(params).subscribe({
        next: (data) => { this.setData(data); },
        error: () => { this.error.set('Error al cargar proveedores'); this.loading.set(false); },
      });
    } else {
      const text = this.searchText().trim();
      const obs = text
        ? this.supplierService.search(text, this.currentPage(), this.pageSize())
        : this.supplierService.getAll(this.currentPage(), this.pageSize());

      obs.subscribe({
        next: (data) => { this.setData(data); },
        error: () => { this.error.set('Error al cargar proveedores'); this.loading.set(false); },
      });
    }
  }

  private setData(data: { content: SupplierResponse[]; pageable: { totalPages: number; totalElements: number } }) {
    this.suppliers.set(data.content);
    this.totalPages.set(data.pageable.totalPages);
    this.totalElements.set(data.pageable.totalElements);
    this.loading.set(false);
  }

  toggleAdvanced() {
    this.showAdvanced.update(v => !v);
  }

  clearFilters() {
    this.searchText.set('');
    this.advName.set('');
    this.advContactName.set('');
    this.advEmail.set('');
    this.advPhone.set('');
    this.currentPage.set(0);
    this.load();
  }

  search() {
    this.currentPage.set(0);
    this.load();
  }

  onSearchKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') this.search();
  }

  onSearchInput(value: string) {
    this.searchText.set(value);
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.load();
  }

  goToPage(page: number) {
    this.currentPage.set(page);
    this.load();
  }

  prevPage() {
    if (this.currentPage() > 0) this.goToPage(this.currentPage() - 1);
  }

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) this.goToPage(this.currentPage() + 1);
  }

  newSupplier() {
    this.router.navigate(['/suppliers/new']);
  }

  editSupplier(uuid: string) {
    this.router.navigate(['/suppliers', uuid]);
  }

  openToggleModal(supplier: SupplierResponse) {
    this.pendingSupplier.set(supplier);
    this.showToggleModal.set(true);
  }

  closeToggleModal() {
    this.showToggleModal.set(false);
    this.pendingSupplier.set(null);
  }

  confirmToggle() {
    const supplier = this.pendingSupplier();
    if (!supplier) return;
    this.closeToggleModal();
    this.supplierService.toggleStatus(supplier.uuid, !supplier.isActive).subscribe({
      next: (updated) => {
        this.suppliers.update(list => list.map(s => s.uuid === updated.uuid ? updated : s));
      },
      error: () => this.error.set('Error al cambiar el estado'),
    });
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  showingFrom(): number {
    return this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1;
  }

  showingTo(): number {
    return Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
  }
}
