import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-time-travel',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './time-travel.component.html',
  styleUrls: ['./time-travel.component.scss']
})
export class TimeTravelComponent {
  request = {
    targetDate: '',
    operation: 'ALL'
  };

  loading = false;
  resultMessage = '';
  error = '';

  constructor(private adminService: AdminService) {}

  executeTimeTravel() {
    if (!confirm('WARNING: This will alter the system date and execute batches up to the target date. Are you absolutely sure?')) {
      return;
    }

    this.loading = true;
    this.error = '';
    this.resultMessage = '';

    this.adminService.timeTravel(this.request).subscribe({
      next: (res) => {
        this.loading = false;
        this.resultMessage = `Time travel executed successfully up to ${this.request.targetDate}`;
      },
      error: () => {
        this.loading = false;
        this.error = 'Time travel execution failed.';
      }
    });
  }
}
