import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AppNotification } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/notifications`;

  list(): Observable<AppNotification[]> {
    return this.http.get<ApiResponse<AppNotification[]>>(this.base).pipe(map(r => r.data));
  }

  unreadCount(): Observable<number> {
    return this.http.get<ApiResponse<{ count: number }>>(`${this.base}/unread-count`).pipe(map(r => r.data.count));
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http.put<ApiResponse<AppNotification>>(`${this.base}/${id}/read`, {}).pipe(map(r => r.data));
  }

  markAllRead(): Observable<void> {
    return this.http.put<ApiResponse<void>>(`${this.base}/read-all`, {}).pipe(map(() => void 0));
  }
}
