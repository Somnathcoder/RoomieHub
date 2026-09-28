import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { RecurringExpense, RecurringExpenseCreateRequest } from '../models/recurring-expense.model';

@Injectable({ providedIn: 'root' })
export class RecurringExpenseService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/recurring-expenses`;

  list(): Observable<RecurringExpense[]> {
    return this.http.get<ApiResponse<RecurringExpense[]>>(this.base).pipe(map(r => r.data));
  }

  create(request: RecurringExpenseCreateRequest): Observable<RecurringExpense> {
    return this.http.post<ApiResponse<RecurringExpense>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: Partial<RecurringExpenseCreateRequest> & { active?: boolean }): Observable<RecurringExpense> {
    return this.http.put<ApiResponse<RecurringExpense>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }
}
