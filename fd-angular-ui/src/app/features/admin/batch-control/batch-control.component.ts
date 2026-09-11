import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-batch-control',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './batch-control.component.html',
  styleUrls: ['./batch-control.component.scss']
})
export class BatchControlComponent {
  jobs = [
    { id: 'accrual', name: 'Interest Accrual', loading: false, result: '' },
    { id: 'maturity', name: 'Maturity Processing', loading: false, result: '' },
    { id: 'statement', name: 'Statement Generation', loading: false, result: '' }
  ];

  constructor(private adminService: AdminService) {}

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
        job.result = `Success: ${res.message || 'Job completed'}`;
      },
      error: () => {
        job.loading = false;
        job.result = 'Failed to execute job.';
      }
    });
  }
}
