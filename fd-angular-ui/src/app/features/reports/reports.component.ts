import { Component, OnInit } from '@angular/core';
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
    private reportService: ReportService
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    if (this.authService.isCustomer()) {
      this.reportService.getPortfolio().subscribe({
        next: (data) => {
          this.portfolioData = data;
          this.loading = false;
        },
        error: () => this.loading = false
      });
    } else {
      this.reportService.getFdSummary().subscribe({
        next: (data) => {
          this.summaryData = data;
          this.loading = false;
        },
        error: () => this.loading = false
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
