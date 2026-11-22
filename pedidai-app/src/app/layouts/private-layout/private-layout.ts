import { Component, HostListener, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from '../../shared/navbar/navbar';
import { Sidebar } from '../../shared/sidebar/sidebar';

@Component({
  selector: 'app-private-layout',
  imports: [RouterOutlet, Navbar, Sidebar],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './private-layout.html',
})
export class PrivateLayoutComponent {
  sidebarOpen = signal(false);
  toggleSidebar() {
    this.sidebarOpen.update((v) => !v);
  }

  /** Esc cierra el menú lateral en móvil y tableta. */
  @HostListener('document:keydown.escape')
  onEscape() {
    this.sidebarOpen.set(false);
  }
}
