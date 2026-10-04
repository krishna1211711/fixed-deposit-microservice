import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FdAccount, FdOpeningRequest, Transaction, Statement } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class FdAccountService {
  private apiUrl = '/api/fd/account';
  private allAccountsUrl = '/api/fd/accounts';

  constructor(private http: HttpClient) {}

  createAccount(request: any, idempotencyKey: string): Observable<any> {
    return this.http.post<FdOpeningRequest>(`/api/fd/opening-requests`, request, {
      headers: { 'Idempotency-Key': idempotencyKey }
    });
  }

  getMyOpeningRequests(): Observable<FdOpeningRequest[]> {
    return this.http.get<FdOpeningRequest[]>('/api/fd/opening-requests/mine');
  }

  getPendingOpeningRequests(): Observable<FdOpeningRequest[]> {
    return this.http.get<FdOpeningRequest[]>('/api/fd/opening-requests/pending');
  }

  approveOpeningRequest(requestId: string, reason = ''): Observable<FdOpeningRequest> {
    return this.http.post<FdOpeningRequest>(`/api/fd/opening-requests/${requestId}/approve`, { reason });
  }

  rejectOpeningRequest(requestId: string, reason: string): Observable<FdOpeningRequest> {
    return this.http.post<FdOpeningRequest>(`/api/fd/opening-requests/${requestId}/reject`, { reason });
  }

  getMyAccounts(): Observable<FdAccount[]> {
    return this.http.get<FdAccount[]>(`${this.allAccountsUrl}/my`);
  }

  getAllAccounts(): Observable<FdAccount[]> {
    return this.http.get<FdAccount[]>(`${this.allAccountsUrl}/all`);
  }

  getAccount(fdAccountNo: string): Observable<FdAccount> {
    return this.http.get<FdAccount>(`${this.apiUrl}/${fdAccountNo}`);
  }

  getTransactions(fdAccountNo: string): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/${fdAccountNo}/transactions`);
  }

  getStatements(fdAccountNo: string): Observable<Statement[]> {
    return this.http.get<Statement[]>(`${this.apiUrl}/${fdAccountNo}/statements`);
  }

  withdraw(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/withdraw`, request);
  }

  manualClose(fdAccountNo: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/manual-close?fdAccountNo=${fdAccountNo}`, {});
  }
}
