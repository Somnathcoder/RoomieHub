import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AdminDashboard, ExpenseAnalytics, MemberDashboard } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/dashboard`;

  getMemberDashboard(): Observable<MemberDashboard> {
    return this.http.get<ApiResponse<MemberDashboard>>(`${this.base}/member`).pipe(map(r => r.data));
  }

  getAdminDashboard(): Observable<AdminDashboard> {
    return this.http.get<ApiResponse<AdminDashboard>>(`${this.base}/admin`).pipe(map(r => r.data));
  }

  getAnalytics(): Observable<ExpenseAnalytics> {
    return this.http.get<ApiResponse<ExpenseAnalytics>>(`${this.base}/analytics`).pipe(map(r => r.data));
  }
}
