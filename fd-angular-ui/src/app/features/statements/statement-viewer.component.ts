import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FdAccountService } from '../../core/services/fd-account.service';
import { Statement } from '../../core/models/models';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-statement-viewer',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './statement-viewer.component.html',
  styleUrls: ['./statement-viewer.component.scss']
})
export class StatementViewerComponent implements OnInit {
  statements: Statement[] = [];
  fdAccountNo = '';
  loading = true;
  error = '';
  maxInterest = 0;

  constructor(
    private route: ActivatedRoute,
    private fdService: FdAccountService
  ) {}

  ngOnInit() {
    this.fdAccountNo = this.route.snapshot.paramMap.get('fdAccountNo') || '';
    if (this.fdAccountNo) {
      this.loadStatements();
    }
  }

  loadStatements() {
    this.fdService.getStatements(this.fdAccountNo).subscribe({
      next: (data) => {
        this.statements = data;
        if (this.statements.length > 0) {
          this.maxInterest = Math.max(...this.statements.map(s => s.interestCredited));
        }
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load statements.';
        this.loading = false;
      }
    });
  }

  getBarHeight(interest: number): string {
    if (this.maxInterest === 0) return '0%';
    const pct = (interest / this.maxInterest) * 100;
    return `${Math.max(5, pct)}%`;
  }
}
