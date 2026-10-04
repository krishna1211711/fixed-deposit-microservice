import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuditLog } from '../../core/models/models';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss']
})
export class AuditLogComponent implements OnInit {
  logs: AuditLog[] = [];
  loading = true;
  error = '';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.http.get<AuditLog[]>('/api/audit/recent').subscribe({
      next: logs => { this.logs = logs; this.loading = false; this.cdr.detectChanges(); },
      error: error => { this.error = error?.error?.message || 'Unable to load audit records.'; this.loading = false; }
    });
  }
}
