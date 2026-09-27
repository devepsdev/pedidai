import { Component, inject, ElementRef, ViewChild, AfterViewChecked, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AiService } from '../../../services/ai';
import { OrderService } from '../../../services/order';
import { AuthService } from '../../../services/auth';
import { LanguageService } from '../../../services/language.service';
import { AiMessage, ChatOrder } from '../../../models/ai.model';
import { formatMoney, formatNumber } from '../../../shared/format';

type SendState = 'idle' | 'sending' | 'sent' | 'error';

/**
 * Pedir por chat: el asistente prepara pedidos PENDIENTES al proveedor más barato
 * y el usuario los revisa y los envía con un clic.
 */
@Component({
  selector: 'app-ai-chat',
  imports: [ReactiveFormsModule, TranslateModule, RouterLink],
  templateUrl: './ai-chat.html',
})
export class AiChat implements OnInit, AfterViewChecked {
  private aiService = inject(AiService);
  private orderService = inject(OrderService);
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
