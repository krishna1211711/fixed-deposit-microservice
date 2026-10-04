import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FdAccountService } from '../../../core/services/fd-account.service';
import { FdOpeningRequest } from '../../../core/models/models';
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-opening-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyFormatPipe],
  templateUrl: './opening-requests.component.html',
  styleUrls: ['./opening-requests.component.scss']
})
export class OpeningRequestsComponent implements OnInit {
  requests: FdOpeningRequest[] = [];
  loading = true;
  error = '';
  message = '';
  reasons: Record<string, string> = {};

  constructor(private fdService: FdAccountService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.fdService.getPendingOpeningRequests().subscribe({
      next: requests => { this.requests = requests; this.loading = false; this.cdr.detectChanges(); },
      error: error => { this.error = error?.error?.message || 'Unable to load the approval queue.'; this.loading = false; }
    });
  }

  approve(request: FdOpeningRequest): void {
    this.decide(request, true, this.reasons[request.requestId] || 'Terms and customer request verified');
  }

  reject(request: FdOpeningRequest): void {
    const reason = (this.reasons[request.requestId] || '').trim();
    if (!reason) { this.error = 'Enter a rejection reason before rejecting.'; return; }
    this.decide(request, false, reason);
  }

  private decide(request: FdOpeningRequest, approve: boolean, reason: string): void {
    this.error = '';
    this.message = '';
    const call = approve
      ? this.fdService.approveOpeningRequest(request.requestId, reason)
      : this.fdService.rejectOpeningRequest(request.requestId, reason);
    call.subscribe({
      next: result => {
        this.message = approve
          ? `Approved. FD ${result.fdAccountNo} was opened atomically.`
          : `Request ${result.requestId} rejected.`;
        this.load();
      },
      error: error => this.error = error?.error?.message || 'The decision could not be recorded.'
    });
  }
}
