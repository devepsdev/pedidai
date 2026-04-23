import { Component, inject, input, output, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth';
import { Subscription, filter } from 'rxjs';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  templateUrl: './sidebar.html',
})
export class Sidebar implements OnInit, OnDestroy {
  open = input<boolean>(true);
  close = output<void>();
  private auth = inject(AuthService);
  private router = inject(Router);
  private navSub?: Subscription;

  get user() { return this.auth.getCurrentUser(); }

  get isSuperAdmin(): boolean {
    return this.user?.role === 'SUPER_ADMIN';
  }

  ngOnInit() {
    // Auto-close sidebar on navigation (mobile only — desktop always open)
    this.navSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        if (this.open()) this.close.emit();
      });
  }

  ngOnDestroy() {
    this.navSub?.unsubscribe();
  }
}
