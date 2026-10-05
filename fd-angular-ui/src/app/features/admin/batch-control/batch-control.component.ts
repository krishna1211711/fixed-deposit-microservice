import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AdminService, BatchRun } from '../../../core/services/admin.service';

@Component({
  selector: 'app-batch-control',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './batch-control.component.html',
  styleUrls: ['./batch-control.component.scss']
})
export class BatchControlComponent implements OnInit {
  businessDate = '';
  timeTravelEnabled = false;
  batchRuns: BatchRun[] = [];
  metadataError = '';
  jobs = [
    { id: 'accrual', name: 'Interest Accrual', loading: false, result: '' },
    { id: 'maturity', name: 'Maturity Processing', loading: false, result: '' },
    { id: 'statement', name: 'Statement Generation', loading: false, result: '' }
  ];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadOperationalMetadata();
  }

  loadOperationalMetadata(): void {
    this.metadataError = '';
    this.adminService.getBusinessDate().subscribe({
      next: status => {
        this.businessDate = status.businessDate;
        this.timeTravelEnabled = status.timeTravelEnabled;
      },
      error: () => this.metadataError = 'Unable to load the banking business date.'
    });
    this.adminService.getBatchRuns().subscribe({
      next: runs => this.batchRuns = runs,
      error: () => this.metadataError = 'Unable to load recent batch runs.'
    });
  }

  runJob(job: any) {
    job.loading = true;
    job.result = '';
    
    let request;
    if (job.id === 'accrual') request = this.adminService.triggerInterestAccrual();
    else if (job.id === 'maturity') request = this.adminService.triggerMaturityProcessing();
    else request = this.adminService.triggerStatementGeneration();

    request.subscribe({
      next: (res: any) => {
        job.loading = false;
        const counts = res.data
          ? ` Found ${res.data.recordsFound}, processed ${res.data.recordsProcessed}, failed ${res.data.recordsFailed}.`
          : '';
        job.result = `Success: ${res.message || 'Job completed'}.${counts}`;
        this.loadOperationalMetadata();
      },
      error: () => {
        job.loading = false;
        job.result = 'Failed to execute job.';
      }
    });
  }
}
