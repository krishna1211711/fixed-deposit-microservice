import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BusinessDateStatus {
  businessDate: string;
  timeTravelEnabled: boolean;
}

export interface BatchRun {
  batch_id: string;
  job_name: string;
  business_date: string;
  status: string;
  attempt_count: number;
  records_found: number;
  records_processed: number;
  records_failed: number;
  triggered_by: string;
  trigger_source: string;
  started_at: string;
  completed_at?: string;
  last_error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  getBusinessDate(): Observable<BusinessDateStatus> {
    return this.http.get<BusinessDateStatus>(`${this.apiUrl}/business-date`);
  }

  getBatchRuns(): Observable<BatchRun[]> {
    return this.http.get<BatchRun[]>(`${this.apiUrl}/batch/runs`);
  }

  triggerInterestAccrual(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/batch/interest-accrual`, {});
  }

  triggerMaturityProcessing(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/batch/maturity-processing`, {});
  }

  triggerStatementGeneration(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/batch/statement-generation`, {});
  }

  timeTravel(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/time-travel`, request);
  }
}
