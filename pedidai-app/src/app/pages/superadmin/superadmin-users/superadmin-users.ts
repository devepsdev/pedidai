import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { SuperAdminService, UserAdmin, PagedResponse } from '../../../services/super-admin.service';
import { avatarClass, companyBadgeClass, initials, roleBadgeClass, userAccess } from '../company-badge';
import { SuperadminHeader } from '../superadmin-header/superadmin-header';

@Component({
  selector: 'app-superadmin-users',
  imports: [RouterLink, FormsModule, TranslateModule, DatePipe, SuperadminHeader],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './superadmin-users.html',
})
export class SuperadminUsers implements OnInit {
  private svc = inject(SuperAdminService);

  readonly access = userAccess;
  readonly badgeClass = companyBadgeClass;
  readonly roleBadge = roleBadgeClass;
  readonly initials = initials;
  readonly avatarClass = avatarClass;

  loading = signal(true);
  users = signal<UserAdmin[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  search = signal('');
  roleFilter = signal('');

  // Plain properties for ngModel binding
  searchValue = '';
  roleFilterValue = '';

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.svc
      .getUsers({
        search: this.search(),
        role: this.roleFilter(),
        page: this.currentPage(),
        size: 20,
      })
      .subscribe({
        next: (r: PagedResponse<UserAdmin>) => {
          this.users.set(r.content);
          this.totalElements.set(r.totalElements);
          this.totalPages.set(r.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  onSearchChange(val: string) {
    this.search.set(val);
    this.applyFilters();
  }

  onRoleFilterChange(val: string) {
    this.roleFilter.set(val);
    this.applyFilters();
  }

  applyFilters() {
    this.currentPage.set(0);
    this.load();
  }
  goToPage(p: number) {
    this.currentPage.set(p);
    this.load();
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
