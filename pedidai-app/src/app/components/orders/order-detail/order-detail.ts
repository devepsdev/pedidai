import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OrderService } from '../../../services/order';
import { SupplierService } from '../../../services/supplier';
import { OrderResponse } from '../../../models/order.model';

@Component({
  selector: 'app-order-detail',
  imports: [TranslateModule],
  templateUrl: './order-detail.html',
})
export class OrderDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orderService = inject(OrderService);
  private supplierService = inject(SupplierService);

  order = signal<OrderResponse | null>(null);
  loading = signal(false);
  error = signal('');
  supplierName = signal('');
  sending = signal(false);
  cancelling = signal(false);
  showCancelConfirm = signal(false);

  ngOnInit() {
    const uuid = this.route.snapshot.paramMap.get('uuid')!;
    this.loading.set(true);
    this.orderService.getByUuid(uuid).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        this.supplierService.getByUuid(order.supplierUuid).subscribe({
          next: (s) => this.supplierName.set(s.name),
          error: () => {},
        });
      },
      error: () => {
        this.error.set('Error al cargar el pedido');
        this.loading.set(false);
      },
    });
  }

  send() {
    const order = this.order();
    if (!order) return;
    this.sending.set(true);
    this.error.set('');
    this.orderService.send(order.uuid).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.sending.set(false);
      },
      error: () => {
        this.error.set('Error al enviar el pedido');
        this.sending.set(false);
      },
    });
  }

  editOrder() {
    const order = this.order();
    if (!order) return;
    this.router.navigate(['/orders', order.uuid, 'edit']);
  }

  confirmCancel() {
    this.showCancelConfirm.set(true);
  }

  dismissCancel() {
    this.showCancelConfirm.set(false);
  }

  cancelOrder() {
    const order = this.order();
    if (!order) return;
    this.cancelling.set(true);
    this.showCancelConfirm.set(false);
    this.error.set('');
    this.orderService.cancel(order.uuid).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.cancelling.set(false);
      },
      error: () => {
        this.error.set('Error al cancelar el pedido');
        this.cancelling.set(false);
      },
    });
  }

  back() {
    this.router.navigate(['/orders']);
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
