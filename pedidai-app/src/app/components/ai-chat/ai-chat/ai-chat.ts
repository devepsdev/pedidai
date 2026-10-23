import { Component, inject, ElementRef, ViewChild, AfterViewChecked, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AiService } from '../../../services/ai';
import { OrderService } from '../../../services/order';
import { SupplierService } from '../../../services/supplier';
import { SupplierResponse } from '../../../models/supplier.model';
import { switchMap } from 'rxjs';
import { AuthService } from '../../../services/auth';
import { LanguageService } from '../../../services/language.service';
import { AiMessage, ChatOrder } from '../../../models/ai.model';
import { formatMoney, formatNumber } from '../../../shared/format';
import { BoldPipe } from '../../../shared/bold.pipe';

type SendState = 'idle' | 'sending' | 'sent' | 'error';

/**
 * Pedir por chat: el asistente prepara pedidos PENDIENTES al proveedor más barato
 * y el usuario los revisa y los envía con un clic.
 */
@Component({
  selector: 'app-ai-chat',
  imports: [ReactiveFormsModule, TranslateModule, RouterLink, BoldPipe],
  templateUrl: './ai-chat.html',
})
export class AiChat implements OnInit, AfterViewChecked {
  private aiService = inject(AiService);
  private orderService = inject(OrderService);
  private supplierService = inject(SupplierService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);
  private language = inject(LanguageService);
  @ViewChild('messagesList') private listEl?: ElementRef<HTMLElement>;

  input = new FormControl('');
  messages = signal<AiMessage[]>([]);
  loading = signal(false);
  sendState = signal<Record<string, SendState>>({});
  sendError = signal<Record<string, string>>({});
  /** Pedidos que no se han podido enviar porque el proveedor no tiene email (se pide aquí mismo). */
  missingEmail = signal<Record<string, SupplierResponse>>({});
  emailDraft = signal<Record<string, string>>({});
  emailError = signal<Record<string, string>>({});
  needsVerification = signal(false);
  verificationSent = signal(false);
  private scrollPending = false;

  ngOnInit() {
    // Viene de "Pedir al más barato" en Mis precios: texto ya escrito
    const q = this.route.snapshot.queryParamMap.get('q');
    if (q) this.input.setValue(q + ' ');
  }

  ngAfterViewChecked() {
    if (this.scrollPending && this.listEl) {
      this.listEl.nativeElement.scrollTop = this.listEl.nativeElement.scrollHeight;
      this.scrollPending = false;
    }
  }

  send() {
    const text = this.input.value?.trim();
    if (!text || this.loading()) return;

    this.push({ role: 'user', content: text, timestamp: new Date() });
    this.input.setValue('');
    this.loading.set(true);

    this.aiService.processOrder({ source: 'chat', body: text }).subscribe({
      next: res => {
        this.push({
          role: 'assistant',
          content: res.message || this.translate.instant(res.orders?.length ? 'AI.CHAT.ORDERS_READY' : 'AI.CHAT.NOTHING_FOUND'),
          timestamp: new Date(),
          orders: res.orders ?? [],
          unmatched: res.unmatched ?? [],
          isError: res.status === 'error',
        });
        this.loading.set(false);
      },
      error: err => {
        const key = err?.status === 429 ? 'AI.CHAT.ERR_RATE_LIMIT' : 'AI.CHAT.ERR_UNAVAILABLE';
        this.push({ role: 'assistant', content: this.translate.instant(key), timestamp: new Date(), isError: true });
        this.loading.set(false);
      },
    });
  }

  /** Envía por email al proveedor un pedido preparado por el asistente. */
  sendOrder(order: ChatOrder) {
    this.setSend(order.uuid, 'sending');
    this.orderService.send(order.uuid).subscribe({
      next: () => this.setSend(order.uuid, 'sent'),
      error: err => {
        if (err?.status === 403) this.needsVerification.set(true);
        this.sendError.update(e => ({ ...e, [order.uuid]: err?.error?.message || this.translate.instant('AI.CHAT.ERR_SEND') }));
        this.setSend(order.uuid, 'error');
        if (err?.status === 400) this.checkSupplierEmail(order.uuid);
      },
    });
  }

  /** Si el envío ha fallado porque el proveedor no tiene email, se le pide al usuario sin salir del chat. */
  private checkSupplierEmail(orderUuid: string) {
    this.orderService.getByUuid(orderUuid)
      .pipe(switchMap(o => this.supplierService.getByUuid(o.supplierUuid)))
      .subscribe(supplier => {
        if (!supplier.email) this.missingEmail.update(m => ({ ...m, [orderUuid]: supplier }));
      });
  }

  setEmailDraft(orderUuid: string, value: string) {
    this.emailDraft.update(d => ({ ...d, [orderUuid]: value }));
    this.emailError.update(e => ({ ...e, [orderUuid]: '' }));
  }

  /** Guarda el email en la ficha del proveedor y vuelve a enviar el pedido. */
  saveEmailAndSend(order: ChatOrder) {
    const supplier = this.missingEmail()[order.uuid];
    const email = (this.emailDraft()[order.uuid] ?? '').trim();
    if (!supplier) return;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.emailError.update(e => ({ ...e, [order.uuid]: this.translate.instant('AI.CHAT.EMAIL_INVALID') }));
      return;
    }
    this.setSend(order.uuid, 'sending');
    const { name, contactName, phone, address, notes } = supplier;
    this.supplierService.update(supplier.uuid, { name, contactName, email, phone, address, notes }).subscribe({
      next: () => {
        // El mismo proveedor puede tener más pedidos en esta conversación: ya no les falta el email
        this.missingEmail.update(m => Object.fromEntries(Object.entries(m).filter(([, s]) => s.uuid !== supplier.uuid)));
        this.sendOrder(order);
      },
      error: err => {
        this.emailError.update(e => ({ ...e, [order.uuid]: err?.error?.message || this.translate.instant('AI.CHAT.ERR_SEND') }));
        this.setSend(order.uuid, 'error');
      },
    });
  }

  resendVerification() {
    this.auth.resendMyVerification().subscribe({
      next: () => this.verificationSent.set(true),
      error: () => this.verificationSent.set(true),
    });
  }

  useExample() {
    this.input.setValue(this.translate.instant('AI.CHAT.EXAMPLE_TEXT'));
  }

  onKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  state(uuid: string): SendState {
    return this.sendState()[uuid] ?? 'idle';
  }

  money(v: number | null | undefined) {
    return formatMoney(v, this.language.current());
  }

  qty(v: number) {
    return formatNumber(v, this.language.current(), 3);
  }

  private setSend(uuid: string, state: SendState) {
    this.sendState.update(s => ({ ...s, [uuid]: state }));
  }

  private push(message: AiMessage) {
    this.messages.update(m => [...m, message]);
    this.scrollPending = true;
  }
}
