import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ReportService } from '../../../services/report';
import { GlobalReport } from '../../../models/dashboard.model';

type QuickRange = 'LAST_WEEK' | 'LAST_MONTH' | 'LAST_YEAR' | '';

@Component({
  selector: 'app-reports',
  imports: [FormsModule, TranslateModule],
  templateUrl: './reports.html',
})
export class Reports implements OnInit {
  private reportService = inject(ReportService);

  report = signal<GlobalReport | null>(null);
  loading = signal(false);
  error = signal('');
  exporting = signal(false);
  startDate = signal('');
  endDate = signal('');
  activeQuickRange = signal<QuickRange>('');

  ngOnInit() {
    this.applyQuickRange('LAST_MONTH');
    this.generate();
  }

  private toDateInput(d: Date): string {
    return d.toISOString().split('T')[0];
  }

  applyQuickRange(range: QuickRange) {
    const now = new Date();
    this.activeQuickRange.set(range);
    if (range === 'LAST_WEEK') {
      const from = new Date(now);
      from.setDate(now.getDate() - 7);
      this.startDate.set(this.toDateInput(from));
      this.endDate.set(this.toDateInput(now));
    } else if (range === 'LAST_MONTH') {
      const from = new Date(now);
      from.setMonth(now.getMonth() - 1);
      this.startDate.set(this.toDateInput(from));
      this.endDate.set(this.toDateInput(now));
    } else if (range === 'LAST_YEAR') {
      const from = new Date(now);
      from.setFullYear(now.getFullYear() - 1);
      this.startDate.set(this.toDateInput(from));
      this.endDate.set(this.toDateInput(now));
    }
  }

  onStartDateInput(value: string) {
    this.startDate.set(value);
    this.activeQuickRange.set('');
  }

  onEndDateInput(value: string) {
    this.endDate.set(value);
    this.activeQuickRange.set('');
  }

  generate() {
    if (!this.startDate() || !this.endDate()) return;
    this.loading.set(true);
    this.error.set('');
    this.report.set(null);
    this.reportService.getGlobal(this.startDate(), this.endDate()).subscribe({
      next: (data) => {
        this.report.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al generar el informe');
        this.loading.set(false);
      },
    });
  }

  exportPdf() {
    if (!this.startDate() || !this.endDate()) return;
    this.exporting.set(true);
    this.reportService.getGlobalPdf(this.startDate(), this.endDate()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `informe_${this.startDate()}_${this.endDate()}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.error.set('Error al exportar el PDF');
        this.exporting.set(false);
      },
    });
  }

  maxSpend(): number {
    const r = this.report();
    if (!r?.despesaProveidors?.length) return 1;
    return Math.max(...r.despesaProveidors.map(s => s.despesaTotal));
  }

  formatAmount(amount: number): string {
    return amount.toFixed(2) + ' €';
  }

  topProducts() {
    const r = this.report();
    if (!r?.topProductes) return [];
    return [...r.topProductes].sort((a, b) => b.quantitatTotal - a.quantitatTotal).slice(0, 10);
  }
}
