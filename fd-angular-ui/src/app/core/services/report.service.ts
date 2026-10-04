import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private apiUrl = '/api/report';

  constructor(private http: HttpClient) {}

  getFdSummary(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/fd-summary`);
  }

  getPortfolio(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/customer-portfolio`);
  }

  exportCsv(customerPortfolio = false): Observable<Blob> {
    const path = customerPortfolio ? '/customer-portfolio/export/csv' : '/export/csv';
    return this.http.get(`${this.apiUrl}${path}`, { responseType: 'blob' });
  }
}
