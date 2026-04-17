import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OrderService } from '../../../services/order';
import { SupplierService } from '../../../services/supplier';
import { ProductService } from '../../../services/product';
import { SupplierResponse } from '../../../models/supplier.model';
import { ProductResponse } from '../../../models/product.model';
import { OrderItemRequest } from '../../../models/order.model';

interface CartItem {
  productUuid: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

@Component({
  selector: 'app-order-create',
  imports: [FormsModule, TranslateModule],
  templateUrl: './order-create.html',
})
export class OrderCreate implements OnInit {
  private orderService = inject(OrderService);
  private supplierService = inject(SupplierService);
  private productService = inject(ProductService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  editUuid = signal<string | null>(null);
  isEdit = signal(false);
  loading = signal(false);
  error = signal('');
  saving = signal(false);
  suppliers = signal<SupplierResponse[]>([]);
  products = signal<ProductResponse[]>([]);
  selectedSupplierUuid = signal('');
  loadingProducts = signal(false);
  orderItems = signal<CartItem[]>([]);
  quantities = signal<Record<string, number>>({});
  name = signal('');
  notes = signal('');
  deliveryDate = signal('');

  totalAmount = computed(() =>
    this.orderItems().reduce((sum, item) => sum + item.subtotal, 0)
  );

  ngOnInit() {
    const uuid = this.route.snapshot.paramMap.get('uuid');
    if (uuid) {
      this.editUuid.set(uuid);
      this.isEdit.set(true);
      this.loading.set(true);
    }

    this.supplierService.getAll(0, 200).subscribe({
      next: (data) => {
        this.suppliers.set(data.content.filter(s => s.isActive));
        if (uuid) this.loadOrderForEdit(uuid);
      },
      error: () => {
        if (uuid) this.loading.set(false);
      },
    });
  }

  private loadOrderForEdit(uuid: string) {
    this.orderService.getByUuid(uuid).subscribe({
      next: (order) => {
        this.name.set(order.name);
        this.notes.set(order.notes ?? '');
        this.deliveryDate.set(order.deliveryDate ?? '');
        this.selectedSupplierUuid.set(order.supplierUuid);
        // Load products for supplier then restore items
        this.productService.filter({ supplierUuid: order.supplierUuid, page: 0, size: 100 }).subscribe({
          next: (data) => {
            this.products.set(data.content.filter(p => p.isActive));
            const items: CartItem[] = order.items.map(i => ({
              productUuid: i.productUuid,
              productName: i.productName,
              unitPrice: i.unitPrice,
              quantity: i.quantity,
              subtotal: i.subtotal,
            }));
            this.orderItems.set(items);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      },
      error: () => {
        this.error.set('Error al cargar el pedido');
        this.loading.set(false);
      },
    });
  }

  onSupplierChange(uuid: string) {
    this.selectedSupplierUuid.set(uuid);
    this.products.set([]);
    this.orderItems.set([]);
    this.quantities.set({});
    if (!uuid) return;
    this.loadingProducts.set(true);
    this.productService.filter({ supplierUuid: uuid, page: 0, size: 100 }).subscribe({
      next: (data) => {
        this.products.set(data.content.filter(p => p.isActive));
        this.loadingProducts.set(false);
      },
      error: () => this.loadingProducts.set(false),
    });
  }

  getQty(productUuid: string): number {
    return this.quantities()[productUuid] ?? 1;
  }

  setQty(productUuid: string, value: string) {
    const n = parseFloat(value);
    this.quantities.update(q => ({ ...q, [productUuid]: isNaN(n) || n <= 0 ? 1 : n }));
  }

  isInCart(productUuid: string): boolean {
    return this.orderItems().some(i => i.productUuid === productUuid);
  }

  addItem(product: ProductResponse) {
    const qty = this.getQty(product.uuid);
    this.orderItems.update(items => {
      const existing = items.find(i => i.productUuid === product.uuid);
      if (existing) {
        const newQty = existing.quantity + qty;
        return items.map(i =>
          i.productUuid === product.uuid
            ? { ...i, quantity: newQty, subtotal: newQty * i.unitPrice }
            : i
        );
      }
      return [
        ...items,
        {
          productUuid: product.uuid,
          productName: product.name,
          unitPrice: product.price,
          quantity: qty,
          subtotal: qty * product.price,
        },
      ];
    });
  }

  removeItem(productUuid: string) {
    this.orderItems.update(items => items.filter(i => i.productUuid !== productUuid));
  }

  onNameInput(value: string) { this.name.set(value); }
  onNotesInput(value: string) { this.notes.set(value); }
  onDeliveryDateInput(value: string) { this.deliveryDate.set(value); }

  submit() {
    this.error.set('');
    if (!this.selectedSupplierUuid()) {
      this.error.set('Selecciona un proveedor');
      return;
    }
    if (this.orderItems().length === 0) {
      this.error.set('Añade al menos un producto al pedido');
      return;
    }
    if (!this.name().trim()) {
      this.error.set('El nombre del pedido es obligatorio');
      return;
    }
    this.saving.set(true);
    const items: OrderItemRequest[] = this.orderItems().map(i => ({
      productUuid: i.productUuid,
      quantity: i.quantity,
    }));
    const payload = {
      supplierUuid: this.selectedSupplierUuid(),
      name: this.name().trim(),
      notes: this.notes().trim() || undefined,
      deliveryDate: this.deliveryDate() || undefined,
      items,
    };

    const uuid = this.editUuid();
    const obs = uuid
      ? this.orderService.update(uuid, payload)
      : this.orderService.create(payload);

    obs.subscribe({
      next: (order) => {
        this.saving.set(false);
        this.router.navigate(['/orders', order.uuid]);
      },
      error: () => {
        this.error.set(uuid ? 'Error al actualizar el pedido' : 'Error al crear el pedido');
        this.saving.set(false);
      },
    });
  }

  cancel() {
    const uuid = this.editUuid();
    if (uuid) {
      this.router.navigate(['/orders', uuid]);
    } else {
      this.router.navigate(['/orders']);
    }
  }

  formatAmount(amount: number): string {
    return amount.toFixed(2) + ' €';
  }
}
