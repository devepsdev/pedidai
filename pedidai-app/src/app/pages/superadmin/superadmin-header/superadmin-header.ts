import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

/** Cabecera común de la zona de administración: título, subtítulo y pestañas. */
@Component({
  selector: 'app-superadmin-header',
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="space-y-5">
      <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3">
        <div>
          <p class="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-amber-700">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"/>
            </svg>
            {{ 'SUPERADMIN.SECTION' | translate }}
          </p>
          <h1 class="mt-1 text-2xl font-bold text-gray-900 [text-wrap:balance]">{{ title() }}</h1>
          @if (subtitle()) {
            <p class="mt-1 text-sm text-gray-500 [text-wrap:pretty]">{{ subtitle() }}</p>
          }
        </div>
        <ng-content />
      </div>
      <nav class="flex gap-1 border-b border-gray-200" [attr.aria-label]="'SUPERADMIN.SECTION' | translate">
        @for (tab of tabs; track tab.link) {
          <a [routerLink]="tab.link" routerLinkActive="!border-amber-500 !text-gray-900"
             [routerLinkActiveOptions]="{ exact: tab.exact }"
             class="-mb-px inline-flex items-center min-h-11 px-3 border-b-2 border-transparent text-sm font-semibold text-gray-500 hover:text-gray-800 transition-colors">
            {{ tab.label | translate }}
          </a>
        }
      </nav>
    </header>
  `,
})
export class SuperadminHeader {
  title = input.required<string>();
  subtitle = input<string>('');

  readonly tabs = [
    { link: '/superadmin', label: 'SUPERADMIN.NAV.DASHBOARD', exact: true },
    { link: '/superadmin/companies', label: 'SUPERADMIN.NAV.COMPANIES', exact: false },
    { link: '/superadmin/users', label: 'SUPERADMIN.NAV.USERS', exact: false },
  ];
}
