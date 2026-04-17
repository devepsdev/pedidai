import { Component, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  templateUrl: './sidebar.html',
})
export class Sidebar {
  open = input<boolean>(true);
  private auth = inject(AuthService);

  get user() { return this.auth.getCurrentUser(); }

  get isSuperAdmin(): boolean {
    return this.user?.role === 'SUPER_ADMIN';
  }
}
