import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FdAccountService } from '../../../core/services/fd-account.service';
import { AuthService } from '../../../core/services/auth.service';
import { FdAccount } from '../../../core/models/models';
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-fd-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './fd-list.component.html',
  styleUrls: ['./fd-list.component.scss']
})
export class FdListComponent implements OnInit {
  accounts: FdAccount[] = [];
  filteredAccounts: FdAccount[] = [];
  
  loading = true;
  searchTerm = '';
  statusFilter = '';

  constructor(
    private fdService: FdAccountService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadAccounts();
  }

  loadAccounts() {
    this.loading = true;
    const request = this.authService.isCustomer() ? 
      this.fdService.getMyAccounts() : 
      this.fdService.getAllAccounts();

    request.subscribe({
      next: (data) => {
        this.accounts = data;
        this.filteredAccounts = [...this.accounts];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filter() {
    this.filteredAccounts = this.accounts.filter(a => {
      const matchSearch = this.searchTerm ? a.fdAccountNo.toLowerCase().includes(this.searchTerm.toLowerCase()) : true;
      const matchStatus = this.statusFilter ? a.status === this.statusFilter : true;
      return matchSearch && matchStatus;
    });
  }
}
