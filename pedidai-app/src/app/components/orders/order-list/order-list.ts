import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OrderService } from '../../../services/order';
import { SupplierService } from '../../../services/supplier';
import { OrderResponse } from '../../../models/order.model';
import { SupplierResponse } from '../../../models/supplier.model';

@Component({
  selector: 'app-order-list',
  imports: [FormsModule, TranslateModule],
  templateUrl: './order-list.html',
})
export class OrderList implements OnInit {
  private orderService = inject(OrderService);
  private supplierService = inject(SupplierService);
  private router = inject(Router);

  orders = signal<OrderResponse[]>([]);
  loading = signal(false);
  error = signal('');
  currentPage = signal(0);
  pageSize = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);
  statusFilter = signal('');
  searchText = signal('');
  supplierMap = signal<Record<string, string>>({});
  suppliers = signal<SupplierResponse[]>([]);

  // Advanced search
  showAdvanced = signal(false);
  advName = signal('');
  advNotes = signal('');
  advSupplierUuid = signal('');
  advMinAmount = signal('');
  advMaxAmount = signal('');
  advDeliveryFrom = signal('');
  advDeliveryTo = signal('');
  advCreatedFrom = signal('');
  advCreatedTo = signal('');

  // Send modal
  showSendModal = signal(false);
  pendingOrder = signal<OrderResponse | null>(null);
  sendingUuid = signal('');

  readonly pageSizeOptions = [5, 10, 25, 50];

  readonly statuses = [
    { value: '', label: 'ORDERS.STATUS_ALL' },
    { value: 'PENDING', label: 'ORDERS.STATUS_PENDING' },
    { value: 'SENT', label: 'ORDERS.STATUS_SENT' },
    { value: 'CONFIRMED', label: 'ORDERS.STATUS_CONFIRMED' },
    { value: 'COMPLETED', label: 'ORDERS.STATUS_COMPLETED' },
    { value: 'CANCELLED', label: 'ORDERS.STATUS_CANCELLED' },
  ];

  ngOnInit() {
    this.supplierService.getAll(0, 200).subscribe({
      next: (data) => {
        const map: Record<string, string> = {};
        data.content.forEach(s => (map[s.uuid] = s.name));
        this.supplierMap.set(map);
        this.suppliers.set(data.content);
      },
      error: () => {},
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    const params: Record<string, string | number | boolean | undefined> = {
      page: this.currentPage(),
      size: this.pageSize(),
      sortBy: 'createdAt',
      sortDir: 'desc',
    };

    if (this.showAdvanced()) {
      if (this.advName().trim()) params['name'] = this.advName().trim();
      if (this.advNotes().trim()) params['notes'] = this.advNotes().trim();
      if (this.statusFilter()) params['status'] = this.statusFilter();
      if (this.advSupplierUuid()) params['supplierUuid'] = this.advSupplierUuid();
      if (this.advMinAmount()) params['minAmount'] = this.advMinAmount();
      if (this.advMaxAmount()) params['maxAmount'] = this.advMaxAmount();
      if (this.advDeliveryFrom()) params['deliveryDateFrom'] = this.advDeliveryFrom();
      if (this.advDeliveryTo()) params['deliveryDateTo'] = this.advDeliveryTo();
      if (this.advCreatedFrom()) params['createdAtFrom'] = this.advCreatedFrom();
      if (this.advCreatedTo()) params['createdAtTo'] = this.advCreatedTo();
    } else {
      if (this.statusFilter()) params['status'] = this.statusFilter();
      if (this.searchText().trim()) params['searchText'] = this.searchText().trim();
    }

    this.orderService.filter(params).subscribe({
      next: (data) => {
        this.orders.set(data.content);
        this.totalPages.set(data.pageable.totalPages);
        this.totalElements.set(data.pageable.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar pedidos');
        this.loading.set(false);
      },
    });
  }

  toggleAdvanced() {
    this.showAdvanced.update(v => !v);
  }

  clearFilters() {
    this.searchText.set('');
    this.statusFilter.set('');
    this.advName.set('');
    this.advNotes.set('');
    this.advSupplierUuid.set('');
    this.advMinAmount.set('');
    this.advMaxAmount.set('');
    this.advDeliveryFrom.set('');
    this.advDeliveryTo.set('');
    this.advCreatedFrom.set('');
    this.advCreatedTo.set('');
    this.currentPage.set(0);
    this.load();
  }

  onStatusChange(value: string) {
    this.statusFilter.set(value);
    this.currentPage.set(0);
    this.load();
  }

  onSearchInput(value: string) {
    this.searchText.set(value);
  }

  search() {
    this.currentPage.set(0);
    this.load();
  }

  onSearchKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') this.search();
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

  pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  showingFrom(): number {
    return this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1;
  }

  showingTo(): number {
    return Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
  }

  newOrder() {
    this.router.navigate(['/orders/new']);
  }

  viewDetail(uuid: string) {
    this.router.navigate(['/orders', uuid]);
  }

  openSendModal(order: OrderResponse) {
    this.pendingOrder.set(order);
    this.showSendModal.set(true);
  }

  closeSendModal() {
    this.showSendModal.set(false);
    this.pendingOrder.set(null);
  }

  confirmSend() {
    const order = this.pendingOrder();
    if (!order) return;
    this.sendingUuid.set(order.uuid);
    this.showSendModal.set(false);
    this.orderService.send(order.uuid).subscribe({
      next: (updated) => {
        this.orders.update(list => list.map(o => (o.uuid === updated.uuid ? updated : o)));
        this.sendingUuid.set('');
        this.pendingOrder.set(null);
      },
      error: () => {
        this.error.set('Error al enviar el pedido');
        this.sendingUuid.set('');
        this.pendingOrder.set(null);
      },
    });
  }

  getSupplierName(uuid: string): string {
    return this.supplierMap()[uuid] ?? '—';
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      SENT: 'bg-blue-100 text-blue-700',
      CONFIRMED: 'bg-green-100 text-green-700',
      COMPLETED: 'bg-emerald-100 text-emerald-800',
      CANCELLED: 'bg-red-100 text-red-700',
      REJECTED: 'bg-red-100 text-red-700',
      DRAFT: 'bg-gray-100 text-gray-600',
      DELETED: 'bg-gray-200 text-gray-500',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Pendiente',
      SENT: 'Enviado',
      CONFIRMED: 'Confirmado',
      COMPLETED: 'Completado',
      CANCELLED: 'Cancelado',
      REJECTED: 'Rechazado',
      DRAFT: 'Borrador',
      DELETED: 'Eliminado',
    };
    return map[status] ?? status;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  formatAmount(amount: number): string {
    return amount.toFixed(2) + ' €';
  }
}
