import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SuperAdminService, CompanyDetail } from '../../../services/super-admin.service';
import { apiError } from '../../../shared/api-error';
import { LanguageService } from '../../../services/language.service';
import { formatMoney } from '../../../shared/format';
import { avatarClass, companyBadge, companyBadgeClass, CompanyBadge, initials, roleBadgeClass } from '../company-badge';

@Component({
  selector: 'app-superadmin-company-detail',
  imports: [RouterLink, TranslateModule, DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './superadmin-company-detail.html',
})
export class SuperadminCompanyDetail implements OnInit {
  private translate = inject(TranslateService);
  private svc = inject(SuperAdminService);
  private route = inject(ActivatedRoute);
  private language = inject(LanguageService);

  money(v: number | null | undefined): string {
    return formatMoney(v, this.language.current());
  }

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
      next: (data) => {
        this.company.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('ERRORS.LOAD_COMPANY'));
        this.loading.set(false);
      },
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
      next: (updated) => {
        this.company.update((prev) => (prev ? { ...prev, status: updated.status } : prev));
        this.showStatusModal.set(false);
        this.saving.set(false);
      },
      error: () => this.saving.set(false),
    });
  }

  readonly badgeClass = companyBadgeClass;
  readonly roleBadge = roleBadgeClass;
  readonly initials = initials;
  readonly avatarClass = avatarClass;

  badge(c: CompanyDetail): CompanyBadge {
    return companyBadge({ ...c, platform: this.isPlatform(c) });
  }

  /** La cuenta de la plataforma (tiene un SUPER_ADMIN) no tiene prueba ni plan. */
  isPlatform(c: { users: { role: string }[] }): boolean {
    return c.users.some((u) => u.role === 'SUPER_ADMIN');
  }

  extendTrial(days: number = 14) {
    const c = this.company();
    if (!c) return;
    this.actionSaving.set(true);
    this.actionError.set('');
    this.svc.extendTrial(c.uuid, days).subscribe({
      next: (updated) => {
        this.company.update((prev) =>
          prev
            ? {
                ...prev,
                status: updated.status,
                trialEndsAt: updated.trialEndsAt ?? undefined,
                trialEnded: updated.trialEnded,
                dataDeletionAt: updated.dataDeletionAt,
              }
            : prev,
        );
        this.actionSaving.set(false);
      },
      error: (err) => {
        this.actionError.set(apiError(err, this.translate.instant('ERRORS.EXTEND_TRIAL')));
        this.actionSaving.set(false);
      },
    });
  }

  activatePro() {
    const c = this.company();
    if (!c) return;
    this.actionSaving.set(true);
    this.actionError.set('');
    this.svc.activatePro(c.uuid).subscribe({
      next: (updated) => {
        this.company.update((prev) =>
          prev
            ? {
                ...prev,
                status: updated.status,
                trialEndsAt: undefined,
                trialEnded: false,
                dataDeletionAt: null,
              }
            : prev,
        );
        this.actionSaving.set(false);
      },
      error: (err) => {
        this.actionError.set(apiError(err, this.translate.instant('ERRORS.ACTIVATE_PLAN')));
        this.actionSaving.set(false);
      },
    });
  }

  orderStatusBadge(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-yellow-50 text-yellow-800 ring-yellow-600/25',
      SENT: 'bg-blue-50 text-blue-700 ring-blue-600/20',
      CONFIRMED: 'bg-sky-50 text-sky-700 ring-sky-600/20',
      COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
      CANCELLED: 'bg-gray-100 text-gray-600 ring-gray-500/20',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600 ring-gray-500/20';
  }
}
