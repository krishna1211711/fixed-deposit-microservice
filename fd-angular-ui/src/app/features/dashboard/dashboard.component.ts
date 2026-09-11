import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { FdAccountService } from '../../core/services/fd-account.service';
import { ReportService } from '../../core/services/report.service';
import { FdAccount } from '../../core/models/models';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  recentFds: FdAccount[] = [];
  summary = {
    totalAccounts: 0,
    totalPrincipal: 0,
    totalInterest: 0,
    activeAccounts: 0
  };
  loading = true;

  constructor(
    public authService: AuthService,
    private fdService: FdAccountService,
    private reportService: ReportService
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    
    if (this.authService.isCustomer()) {
      this.fdService.getMyAccounts().subscribe({
        next: (accounts) => {
          this.recentFds = accounts.slice(0, 5);
          this.calculateSummary(accounts);
          this.loading = false;
        },
        error: () => this.loading = false
      });
    } else {
      this.reportService.getFdSummary().subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            this.summary.totalAccounts = data.reduce((sum: number, item: any) => sum + item.totalAccounts, 0);
            this.summary.totalPrincipal = data.reduce((sum: number, item: any) => sum + item.totalPrincipalAmount, 0);
            this.summary.activeAccounts = this.summary.totalAccounts; // Approximate for admin view
          }
          this.loading = false;
        },
        error: () => this.loading = false
      });
      
      this.fdService.getAllAccounts().subscribe({
        next: (accounts) => {
          this.recentFds = accounts.slice(0, 5);
        }
      });
    }
  }

  private calculateSummary(accounts: FdAccount[]) {
    this.summary.totalAccounts = accounts.length;
    this.summary.activeAccounts = accounts.filter(a => a.status === 'ACTIVE').length;
    this.summary.totalPrincipal = accounts.reduce((sum, a) => sum + a.principalAmount, 0);
  }
}
