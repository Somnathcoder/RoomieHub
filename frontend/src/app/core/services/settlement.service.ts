import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { MemberBalance, Settlement, SettlementSummary } from '../models/settlement.model';

@Injectable({ providedIn: 'root' })
export class SettlementService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/settlements`;

  list(status?: string): Observable<Settlement[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<ApiResponse<Settlement[]>>(this.base, params).pipe(map(r => r.data));
  }

  pay(id: number, paymentDate?: string): Observable<Settlement> {
    return this.http.post<ApiResponse<Settlement>>(`${this.base}/${id}/pay`, { paymentDate }).pipe(map(r => r.data));
  }

  verify(id: number): Observable<Settlement> {
    return this.http.post<ApiResponse<Settlement>>(`${this.base}/${id}/verify`, {}).pipe(map(r => r.data));
  }

  balances(): Observable<MemberBalance[]> {
    return this.http.get<ApiResponse<MemberBalance[]>>(`${this.base}/balances`).pipe(map(r => r.data));
  }

  summary(): Observable<SettlementSummary[]> {
    return this.http.get<ApiResponse<SettlementSummary[]>>(`${this.base}/summary`).pipe(map(r => r.data));
  }
}
