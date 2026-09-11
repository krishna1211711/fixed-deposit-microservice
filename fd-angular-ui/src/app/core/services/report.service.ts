import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private apiUrl = 'http://localhost:8080/api/report';

  constructor(private http: HttpClient) {}

  getFdSummary(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/fd-summary`);
  }

  getPortfolio(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/customer-portfolio`);
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/csv`, { responseType: 'blob' });
  }
}
