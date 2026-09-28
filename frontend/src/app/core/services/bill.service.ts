import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Bill, BillCreateRequest } from '../models/bill.model';

@Injectable({ providedIn: 'root' })
export class BillService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/bills`;

  list(status?: string): Observable<Bill[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<ApiResponse<Bill[]>>(this.base, params).pipe(map(r => r.data));
  }

  create(request: BillCreateRequest): Observable<Bill> {
    return this.http.post<ApiResponse<Bill>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: Partial<Bill> & { paidByMemberId?: number }): Observable<Bill> {
    return this.http.put<ApiResponse<Bill>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }

  uploadPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/photo`, form).pipe(map(r => r.data));
  }
}
