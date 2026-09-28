import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Issue, IssueCreateRequest } from '../models/issue.model';

@Injectable({ providedIn: 'root' })
export class IssueService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/issues`;

  list(status?: string): Observable<Issue[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<ApiResponse<Issue[]>>(this.base, params).pipe(map(r => r.data));
  }

  create(request: IssueCreateRequest): Observable<Issue> {
    return this.http.post<ApiResponse<Issue>>(this.base, request).pipe(map(r => r.data));
  }

  updateStatus(id: number, status: string): Observable<Issue> {
    return this.http.put<ApiResponse<Issue>>(`${this.base}/${id}`, { status }).pipe(map(r => r.data));
  }

  uploadPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/photo`, form).pipe(map(r => r.data));
  }
}
