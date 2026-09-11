import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  currentLang = 'en';

  constructor(
    public authService: AuthService,
    private translate: TranslateService
  ) {}

  toggleLanguage() {
    this.currentLang = this.currentLang === 'en' ? 'hi' : 'en';
    this.translate.use(this.currentLang);
  }

  logout() {
    this.authService.logout();
  }
}
