import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ReportService } from '../../core/services/report.service';
import { AuthService } from '../../core/services/auth.service';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {
  summaryData: any[] = [];
  portfolioData: any = null;
  loading = true;

  constructor(
    public authService: AuthService,
    private reportService: ReportService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    if (this.authService.isCustomer()) {
      this.reportService.getPortfolio().subscribe({
        next: (data: any[]) => {
          const accounts = Array.isArray(data) ? data : [];
          this.portfolioData = {
            totalInvestments: accounts.reduce((sum, item) => sum + Number(item.principalAmount || 0), 0),
            totalInterestEarned: accounts.reduce((sum, item) => sum + Number(item.accruedInterest || 0), 0),
            activeAccountsCount: accounts.filter(item => item.status === 'ACTIVE').length
          };
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.reportService.getFdSummary().subscribe({
        next: (data) => {
          this.summaryData = data;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  exportCsv() {
    this.reportService.exportCsv().subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report_${new Date().toISOString().split('T')[0]}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
