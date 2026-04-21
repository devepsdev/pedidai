import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SuperAdminService, CompanyDetail } from '../../../services/super-admin.service';

@Component({
  selector: 'app-superadmin-company-detail',
  imports: [RouterLink, TranslateModule, DatePipe, CurrencyPipe, FormsModule],
  templateUrl: './superadmin-company-detail.html',
})
export class SuperadminCompanyDetail implements OnInit {
  private svc = inject(SuperAdminService);
  private route = inject(ActivatedRoute);

  loading = signal(true);
  saving = signal(false);
  actionSaving = signal(false);
  error = signal('');
  actionError = signal('');
  company = signal<CompanyDetail | null>(null);
  showStatusModal = signal(false);
  newStatus = signal('');

  // Plain property for ngModel binding
  newStatusValue = '';

  ngOnInit() {
    const uuid = this.route.snapshot.paramMap.get('uuid')!;
    this.svc.getCompany(uuid).subscribe({
      next: data => { this.company.set(data); this.loading.set(false); },
      error: () => { this.error.set('Error al cargar empresa'); this.loading.set(false); }
    });
  }

  openStatusModal() {
    const status = this.company()?.status ?? '';
    this.newStatus.set(status);
    this.newStatusValue = status;
    this.showStatusModal.set(true);
  }

  onNewStatusChange(val: string) {
    this.newStatus.set(val);
  }

  confirmStatusChange() {
    const c = this.company();
    if (!c) return;
    this.saving.set(true);
    this.svc.updateCompanyStatus(c.uuid, this.newStatus()).subscribe({
      next: updated => {
        this.company.update(prev => prev ? { ...prev, status: updated.status } : prev);
        this.showStatusModal.set(false);
        this.saving.set(false);
      },
      error: () => this.saving.set(false)
    });
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-500/20 text-green-400';
      case 'INACTIVE': return 'bg-gray-500/20 text-gray-400';
      case 'SUSPENDED': return 'bg-red-500/20 text-red-400';
      default: return 'bg-yellow-500/20 text-yellow-400';
    }
  }

  roleBadge(role: string): string {
    return role === 'ADMIN' ? 'bg-blue-500/20 text-blue-400' :
           role === 'SUPER_ADMIN' ? 'bg-amber-500/20 text-amber-400' :
           'bg-slate-600/50 text-slate-400';
  }

  extendTrial(months: number = 3) {
    const c = this.company();
    if (!c) return;
    this.actionSaving.set(true);
    this.actionError.set('');
    this.svc.extendTrial(c.uuid, months).subscribe({
      next: updated => {
        this.company.update(prev => prev ? {
          ...prev,
          status: updated.status,
          trialEndsAt: updated.trialEndsAt ?? undefined
        } : prev);
        this.actionSaving.set(false);
      },
      error: () => {
        this.actionError.set('Error al extender el trial');
        this.actionSaving.set(false);
      }
    });
  }

  activatePro() {
    const c = this.company();
    if (!c) return;
    this.actionSaving.set(true);
    this.actionError.set('');
    this.svc.activatePro(c.uuid).subscribe({
      next: updated => {
        this.company.update(prev => prev ? {
          ...prev,
          status: updated.status,
          trialEndsAt: undefined
        } : prev);
        this.actionSaving.set(false);
      },
      error: () => {
        this.actionError.set('Error al activar Plan Pro');
        this.actionSaving.set(false);
      }
    });
  }

  orderStatusBadge(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-yellow-500/20 text-yellow-400',
      SENT: 'bg-blue-500/20 text-blue-400',
      CONFIRMED: 'bg-cyan-500/20 text-cyan-400',
      COMPLETED: 'bg-green-500/20 text-green-400',
      CANCELLED: 'bg-gray-500/20 text-gray-400',
    };
    return map[status] ?? 'bg-slate-600/50 text-slate-400';
  }
}
