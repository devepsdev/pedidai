import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AiService } from '../../../services/ai';
import { OrderService } from '../../../services/order';
import { AiSuggestion } from '../../../models/ai.model';

@Component({
  selector: 'app-ai-suggestions',
  imports: [TranslateModule],
  templateUrl: './ai-suggestions.html',
})
export class AiSuggestions implements OnInit {
  private ai = inject(AiService);
  private orderService = inject(OrderService);

  suggestions = signal<AiSuggestion[]>([]);
  loading = signal(true);
  error = signal('');

  confirmIndex = signal<number | null>(null);
  creatingIndex = signal<number | null>(null);
  createdUuids = signal<Set<number>>(new Set());

  ngOnInit() {
    this.ai.suggestOrders().subscribe({
      next: (data) => { this.suggestions.set(data ?? []); this.loading.set(false); },
      error: () => { this.error.set('Servicio IA no disponible'); this.loading.set(false); }
    });
  }

  urgencyClass(urgency: string): string {
    return urgency === 'high' ? 'bg-red-100 text-red-700 border-red-200'
      : urgency === 'medium' ? 'bg-orange-100 text-orange-700 border-orange-200'
      : 'bg-green-100 text-green-700 border-green-200';
  }

  urgencyLabel(urgency: string): string {
    return urgency === 'high' ? 'Urgente' : urgency === 'medium' ? 'Medio' : 'Bajo';
  }

  openConfirm(index: number) { this.confirmIndex.set(index); }
  closeConfirm() { this.confirmIndex.set(null); }

  createOrder(index: number) {
    const s = this.suggestions()[index];
    this.closeConfirm();
    this.creatingIndex.set(index);

    this.orderService.create({
      supplierUuid: s.supplier_uuid ?? '',
      name: `Pedido sugerido: ${s.product}`,
      notes: `Sugerencia IA - Ahorro estimado: ${s.estimated_savings_percent ?? 0}%`,
      items: s.product_uuid ? [{ productUuid: s.product_uuid, quantity: s.quantity }] : []
    }).subscribe({
      next: () => {
        this.createdUuids.set(new Set([...this.createdUuids(), index]));
        this.creatingIndex.set(null);
      },
      error: () => { this.creatingIndex.set(null); }
    });
  }
}
