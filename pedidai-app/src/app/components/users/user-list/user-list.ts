import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { UserService } from '../../../services/user';
import { UserResponse } from '../../../models/user.model';

@Component({
  selector: 'app-user-list',
  imports: [FormsModule, TranslateModule],
  templateUrl: './user-list.html',
})
export class UserList implements OnInit {
  private userService = inject(UserService);
  private router = inject(Router);

  users = signal<UserResponse[]>([]);
  loading = signal(false);
  error = signal('');
  searchText = signal('');
  currentPage = signal(0);
  pageSize = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);

  // Toggle modal
  showToggleModal = signal(false);
  pendingUser = signal<UserResponse | null>(null);

  // Advanced search
  showAdvanced = signal(false);
  advFirstName = signal('');
  advLastName = signal('');
  advEmail = signal('');
  advPhone = signal('');

  readonly pageSizeOptions = [5, 10, 25, 50];

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');

    if (this.showAdvanced()) {
      const params: Record<string, string | number | undefined> = {
        page: this.currentPage(),
        size: this.pageSize(),
        sortBy: 'firstName',
        sortDir: 'asc',
      };
      if (this.advFirstName().trim()) params['firstName'] = this.advFirstName().trim();
      if (this.advLastName().trim()) params['lastName'] = this.advLastName().trim();
      if (this.advEmail().trim()) params['email'] = this.advEmail().trim();
      if (this.advPhone().trim()) params['phone'] = this.advPhone().trim();

      this.userService.filter(params).subscribe({
        next: (data) => { this.setData(data); },
        error: () => { this.error.set('Error al cargar usuarios'); this.loading.set(false); },
      });
    } else {
      const text = this.searchText().trim();
      const obs = text
        ? this.userService.search(text, this.currentPage(), this.pageSize())
        : this.userService.getAll(this.currentPage(), this.pageSize());

      obs.subscribe({
        next: (data) => { this.setData(data); },
        error: () => { this.error.set('Error al cargar usuarios'); this.loading.set(false); },
      });
    }
  }

  private setData(data: { content: UserResponse[]; pageable: { totalPages: number; totalElements: number } }) {
    this.users.set(data.content);
    this.totalPages.set(data.pageable.totalPages);
    this.totalElements.set(data.pageable.totalElements);
    this.loading.set(false);
  }

  toggleAdvanced() {
    this.showAdvanced.update(v => !v);
  }

  clearFilters() {
    this.searchText.set('');
    this.advFirstName.set('');
    this.advLastName.set('');
    this.advEmail.set('');
    this.advPhone.set('');
    this.currentPage.set(0);
    this.load();
  }

  search() {
    this.currentPage.set(0);
    this.load();
  }

  onSearchInput(value: string) {
    this.searchText.set(value);
  }

  onSearchKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') this.search();
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.load();
  }

  goToPage(page: number) {
    this.currentPage.set(page);
    this.load();
  }

  prevPage() {
    if (this.currentPage() > 0) this.goToPage(this.currentPage() - 1);
  }

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) this.goToPage(this.currentPage() + 1);
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  showingFrom(): number {
    return this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1;
  }

  showingTo(): number {
    return Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
  }

  newUser() {
    this.router.navigate(['/users/new']);
  }

  editUser(uuid: string) {
    this.router.navigate(['/users', uuid]);
  }

  openToggleModal(user: UserResponse) {
    this.pendingUser.set(user);
    this.showToggleModal.set(true);
  }

  closeToggleModal() {
    this.showToggleModal.set(false);
    this.pendingUser.set(null);
  }

  confirmToggle() {
    const user = this.pendingUser();
    if (!user) return;
    this.closeToggleModal();
    this.userService.toggleStatus(user.uuid, !user.isActive).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => (u.uuid === updated.uuid ? updated : u)));
      },
      error: () => this.error.set('Error al cambiar el estado'),
    });
  }
}
