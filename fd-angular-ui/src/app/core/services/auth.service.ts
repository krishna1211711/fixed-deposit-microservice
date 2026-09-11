import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { User } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/auth';
  private tokenKey = 'fd_auth_token';
  
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromToken();
  }

  login(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        if (res.token) {
          localStorage.setItem(this.tokenKey, res.token);
          this.loadUserFromToken();
        }
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, userData);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private loadUserFromToken(): void {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        const rawRoles = decoded.roles ?? (decoded.role ? [decoded.role] : []);
        const user: User = {
          username: decoded.sub,
          email: decoded.email || '',
          roles: rawRoles.map((role: string) => role.replace(/^ROLE_/, ''))
        };
        this.currentUserSubject.next(user);
      } catch (e) {
        this.logout();
      }
    }
  }

  getRole(): string {
    const user = this.currentUserSubject.value;
    if (user && user.roles && user.roles.length > 0) {
      return user.roles[0];
    }
    return '';
  }

  getUsername(): string {
    return this.currentUserSubject.value?.username || '';
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  isOfficer(): boolean {
    return this.getRole() === 'BANK_OFFICER';
  }

  isCustomer(): boolean {
    return this.getRole() === 'CUSTOMER';
  }
}
