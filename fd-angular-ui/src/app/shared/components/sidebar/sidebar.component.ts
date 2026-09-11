import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  isExpanded = true;

  constructor(public authService: AuthService, private router: Router) {}

  isActive(route: string): boolean {
    return this.router.url.includes(route);
  }

  toggleSidebar() {
    this.isExpanded = !this.isExpanded;
  }
}
