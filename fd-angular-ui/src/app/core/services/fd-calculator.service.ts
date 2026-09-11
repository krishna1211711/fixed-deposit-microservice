import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FdSimulationRequest, FdSimulationResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class FdCalculatorService {
  private apiUrl = 'http://localhost:8080/api/fd/calculator';

  constructor(private http: HttpClient) {}

  simulate(request: FdSimulationRequest): Observable<FdSimulationResponse> {
    return this.http.post<FdSimulationResponse>(`${this.apiUrl}/simulate`, request);
  }
}
