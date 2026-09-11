import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FdAccount, Transaction, Statement } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class FdAccountService {
  private apiUrl = 'http://localhost:8080/api/fd/account';
  private allAccountsUrl = 'http://localhost:8080/api/fd/accounts';

  constructor(private http: HttpClient) {}

  createAccount(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/create`, request);
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
