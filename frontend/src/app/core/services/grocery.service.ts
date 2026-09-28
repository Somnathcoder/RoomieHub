import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ShoppingItem, ShoppingItemCreateRequest } from '../models/shopping.model';
import { Expense, SplitItemRequest } from '../models/expense.model';

@Injectable({ providedIn: 'root' })
export class GroceryService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/shopping-items`;

  list(status?: string): Observable<ShoppingItem[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<ApiResponse<ShoppingItem[]>>(this.base, params).pipe(map(r => r.data));
  }

  add(request: ShoppingItemCreateRequest): Observable<ShoppingItem> {
    return this.http.post<ApiResponse<ShoppingItem>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: Partial<ShoppingItem>): Observable<ShoppingItem> {
    return this.http.put<ApiResponse<ShoppingItem>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }

  uploadItemPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/item-photo`, form).pipe(map(r => r.data));
  }

  uploadBillPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/bill-photo`, form).pipe(map(r => r.data));
  }

  convertToExpense(id: number, actualAmount: number, paidByMemberId: number, splitType: string,
                    customSplits?: SplitItemRequest[], equalSplitMemberIds?: number[]): Observable<Expense> {
    return this.http.post<ApiResponse<Expense>>(`${this.base}/${id}/convert-to-expense`,
      { actualAmount, paidByMemberId, splitType, customSplits, equalSplitMemberIds }).pipe(map(r => r.data));
  }
}
