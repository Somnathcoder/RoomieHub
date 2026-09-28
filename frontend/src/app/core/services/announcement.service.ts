import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Announcement, AnnouncementCreateRequest } from '../models/announcement.model';

@Injectable({ providedIn: 'root' })
export class AnnouncementService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/announcements`;

  list(): Observable<Announcement[]> {
    return this.http.get<ApiResponse<Announcement[]>>(this.base).pipe(map(r => r.data));
  }

  create(request: AnnouncementCreateRequest): Observable<Announcement> {
    return this.http.post<ApiResponse<Announcement>>(this.base, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }
}
