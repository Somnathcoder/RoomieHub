import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Poll, PollCreateRequest } from '../models/poll.model';

@Injectable({ providedIn: 'root' })
export class PollService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/polls`;

  list(): Observable<Poll[]> {
    return this.http.get<ApiResponse<Poll[]>>(this.base).pipe(map(r => r.data));
  }

  create(request: PollCreateRequest): Observable<Poll> {
    return this.http.post<ApiResponse<Poll>>(this.base, request).pipe(map(r => r.data));
  }

  vote(id: number, optionId: number): Observable<Poll> {
    return this.http.post<ApiResponse<Poll>>(`${this.base}/${id}/vote`, { optionId }).pipe(map(r => r.data));
  }

  close(id: number): Observable<Poll> {
    return this.http.put<ApiResponse<Poll>>(`${this.base}/${id}/close`, {}).pipe(map(r => r.data));
  }
}
