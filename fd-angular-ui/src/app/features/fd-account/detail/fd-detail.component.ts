import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FdAccountService } from '../../../core/services/fd-account.service';
import { FdAccount, Transaction } from '../../../core/models/models';
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-fd-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './fd-detail.component.html',
  styleUrls: ['./fd-detail.component.scss']
})
export class FdDetailComponent implements OnInit {
  account: FdAccount | null = null;
  transactions: Transaction[] = [];
  
  activeTab = 'overview';
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private fdService: FdAccountService,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('fdAccountNo');
    if (id) {
      this.loadData(id);
    }
  }

  loadData(id: string) {
    this.loading = true;
    this.fdService.getAccount(id).subscribe({
      next: (acc) => {
        this.account = acc;
        this.loadTransactions(id);
      },
      error: () => {
        this.error = 'Account not found.';
        this.loading = false;
      }
    });
  }

  loadTransactions(id: string) {
    this.fdService.getTransactions(id).subscribe({
      next: (txs) => {
        this.transactions = txs;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  setTab(tab: string) {
    this.activeTab = tab;
  }
}
