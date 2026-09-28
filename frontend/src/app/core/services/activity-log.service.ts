import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ActivityLog } from '../models/activity-log.model';

@Injectable({ providedIn: 'root' })
export class ActivityLogService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/activity-logs`;

  list(filters: { userId?: number; module?: string; from?: string; to?: string } = {}): Observable<ActivityLog[]> {
    // HttpClient stringifies every value in a plain params object, so an unset
    // filter (undefined) would otherwise be sent as the literal text "undefined"
    // and the backend's @RequestParam Long/String binding would reject it.
    const params: Record<string, string> = {};
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params[key] = String(value);
      }
    }
    return this.http.get<ApiResponse<ActivityLog[]>>(this.base, { params }).pipe(map(r => r.data));
  }
}
