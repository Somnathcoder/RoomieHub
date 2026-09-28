import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Expense, ExpenseCreateRequest } from '../models/expense.model';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/expenses`;

  list(status?: string): Observable<Expense[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<ApiResponse<Expense[]>>(this.base, params).pipe(map(r => r.data));
  }

  get(id: number): Observable<Expense> {
    return this.http.get<ApiResponse<Expense>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  create(request: ExpenseCreateRequest): Observable<Expense> {
    return this.http.post<ApiResponse<Expense>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: Partial<ExpenseCreateRequest>): Observable<Expense> {
    return this.http.put<ApiResponse<Expense>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }

  decide(id: number, action: 'APPROVE' | 'REJECT', rejectionReason?: string): Observable<Expense> {
    return this.http.post<ApiResponse<Expense>>(`${this.base}/${id}/approve`, { action, rejectionReason }).pipe(map(r => r.data));
  }

  uploadReceipt(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/receipt`, form).pipe(map(r => r.data));
  }
}
