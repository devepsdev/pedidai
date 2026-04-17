import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ProductService } from '../../../services/product';
import { SupplierService } from '../../../services/supplier';
import { ProductResponse } from '../../../models/product.model';
import { SupplierResponse } from '../../../models/supplier.model';

@Component({
  selector: 'app-product-list',
  imports: [FormsModule, TranslateModule],
  templateUrl: './product-list.html',
})
export class ProductList implements OnInit {
  private productService = inject(ProductService);
  private supplierService = inject(SupplierService);
  private router = inject(Router);

  products = signal<ProductResponse[]>([]);
  loading = signal(false);
  error = signal('');
  searchText = signal('');
  currentPage = signal(0);
  pageSize = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);
  suppliers = signal<SupplierResponse[]>([]);

  // Deactivate modal
  showDeactivateModal = signal(false);
  pendingProduct = signal<ProductResponse | null>(null);

  // Advanced search
  showAdvanced = signal(false);
  advName = signal('');
  advSupplierUuid = signal('');
  advCategory = signal('');
  advMinPrice = signal('');
  advMaxPrice = signal('');
  advActive = signal('');

  readonly pageSizeOptions = [5, 10, 25, 50];

  ngOnInit() {
    this.supplierService.getAll(0, 200).subscribe({
      next: (data) => this.suppliers.set(data.content),
      error: () => {},
    });
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
      if (this.advSupplierUuid()) params['supplierUuid'] = this.advSupplierUuid();
      if (this.advCategory().trim()) params['category'] = this.advCategory().trim();
      if (this.advMinPrice()) params['minPrice'] = this.advMinPrice();
      if (this.advMaxPrice()) params['maxPrice'] = this.advMaxPrice();
      if (this.advActive() !== '') params['isActive'] = this.advActive();

      this.productService.filter(params).subscribe({
        next: (data) => { this.setData(data); },
        error: () => { this.error.set('Error al cargar productos'); this.loading.set(false); },
      });
    } else {
      const text = this.searchText().trim();
      const obs = text
        ? this.productService.search(text, this.currentPage(), this.pageSize())
        : this.productService.getAll(this.currentPage(), this.pageSize());

      obs.subscribe({
        next: (data) => { this.setData(data); },
        error: () => { this.error.set('Error al cargar productos'); this.loading.set(false); },
      });
    }
  }

  private setData(data: { content: ProductResponse[]; pageable: { totalPages: number; totalElements: number } }) {
    this.products.set(data.content);
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
    this.advSupplierUuid.set('');
    this.advCategory.set('');
    this.advMinPrice.set('');
    this.advMaxPrice.set('');
    this.advActive.set('');
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

  newProduct() {
    this.router.navigate(['/products/new']);
  }

  editProduct(uuid: string) {
    this.router.navigate(['/products', uuid]);
  }

  openDeactivateModal(product: ProductResponse) {
    this.pendingProduct.set(product);
    this.showDeactivateModal.set(true);
  }

  closeDeactivateModal() {
    this.showDeactivateModal.set(false);
    this.pendingProduct.set(null);
  }

  confirmDeactivate() {
    const product = this.pendingProduct();
    if (!product) return;
    this.closeDeactivateModal();
    this.productService.deactivate(product.uuid).subscribe({
      next: (updated) => {
        this.products.update(list => list.map(p => p.uuid === updated.uuid ? updated : p));
      },
      error: () => this.error.set('Error al desactivar el producto'),
    });
  }

  formatPrice(price: number): string {
    return price.toFixed(2) + ' €';
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
