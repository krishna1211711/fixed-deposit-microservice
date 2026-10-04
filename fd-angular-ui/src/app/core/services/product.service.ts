import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = '/api/product';

  constructor(private http: HttpClient) {}

  getProducts(type?: string, status?: string): Observable<Product[]> {
    let queryParams = [];
    if (type) queryParams.push(`type=${type}`);
    if (status) queryParams.push(`status=${status}`);
    const qs = queryParams.length > 0 ? `?${queryParams.join('&')}` : '';
    
    return this.http.get<Product[]>(`${this.apiUrl}/search${qs}`);
  }

  getProduct(code: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${code}`);
  }

  getAllProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/all`);
  }

  createProduct(request: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, request);
  }

  updateProduct(code: string, request: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${code}`, request);
  }
}
