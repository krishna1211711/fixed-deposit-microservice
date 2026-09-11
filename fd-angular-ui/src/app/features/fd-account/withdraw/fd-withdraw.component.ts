import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FdAccountService } from '../../../core/services/fd-account.service';
import { FdAccount } from '../../../core/models/models';
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-fd-withdraw',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './fd-withdraw.component.html',
  styleUrls: ['./fd-withdraw.component.scss']
})
export class FdWithdrawComponent implements OnInit {
  account: FdAccount | null = null;
  
  request = {
    fdAccountNo: '',
    targetAccountNo: '',
    remarks: 'Premature Withdrawal'
  };

  loading = false;
  successMessage = '';
  error = '';

  constructor(
    private route: ActivatedRoute,
    private fdService: FdAccountService,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('fdAccountNo');
    if (id) {
      this.request.fdAccountNo = id;
      this.loadAccount(id);
    }
  }

  loadAccount(id: string) {
    this.fdService.getAccount(id).subscribe({
      next: (acc) => this.account = acc,
      error: () => this.error = 'Failed to load account details'
    });
  }

  onSubmit() {
    if (!confirm('Are you sure you want to withdraw this FD? This action cannot be undone and may incur penalties.')) {
      return;
    }
    
    this.loading = true;
    this.error = '';
    
    this.fdService.withdraw(this.request).subscribe({
      next: (res) => {
        this.successMessage = 'Withdrawal successful!';
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/fd', this.account?.fdAccountNo]);
        }, 2000);
      },
      error: (err) => {
        this.error = 'Failed to process withdrawal.';
        this.loading = false;
      }
    });
  }
}
