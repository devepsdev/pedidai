import { Component, inject, ElementRef, ViewChild, AfterViewChecked, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AiService } from '../../../services/ai';
import { AiMessage, AiOrderResponse } from '../../../models/ai.model';

@Component({
  selector: 'app-ai-chat',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './ai-chat.html',
})
export class AiChat implements AfterViewChecked {
  private aiService = inject(AiService);
  @ViewChild('messagesList') private listEl!: ElementRef;

  input = new FormControl('');
  messages = signal<AiMessage[]>([]);
  loading = signal(false);
  lastResponse = signal<AiOrderResponse | null>(null);

  ngAfterViewChecked() {
    if (this.listEl) {
      this.listEl.nativeElement.scrollTop = this.listEl.nativeElement.scrollHeight;
    }
  }

  send() {
    const text = this.input.value?.trim();
    if (!text || this.loading()) return;

    this.messages.update(msgs => [...msgs, { role: 'user', content: text, timestamp: new Date() }]);
    this.input.setValue('');
    this.loading.set(true);
    this.lastResponse.set(null);

    this.aiService.processOrder({ source: 'chat', body: text }).subscribe({
      next: (res) => {
        this.lastResponse.set(res);
        this.messages.update(msgs => [...msgs, { role: 'assistant', content: res.message, timestamp: new Date() }]);
        this.loading.set(false);
      },
      error: (err) => {
        this.messages.update(msgs => [...msgs, {
          role: 'assistant',
          content: err.error?.message || 'Error al procesar el pedido. Verifica que el servicio IA está activo.',
          timestamp: new Date()
        }]);
        this.loading.set(false);
      }
    });
  }

  onKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  responseCardClass(): string {
    if (!this.lastResponse()) return '';
    return this.lastResponse()!.status === 'success' ? 'bg-green-50 border-green-200'
      : this.lastResponse()!.status === 'duplicate' ? 'bg-orange-50 border-orange-200'
      : 'bg-red-50 border-red-200';
  }
}
