import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

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
