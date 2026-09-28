import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { CleaningRotation, CleaningRotationCreateRequest, CleaningSchedule, CleaningScheduleCreateRequest } from '../models/cleaning.model';

@Injectable({ providedIn: 'root' })
export class CleaningService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  listSchedules(mine = false): Observable<CleaningSchedule[]> {
    return this.http.get<ApiResponse<CleaningSchedule[]>>(`${this.apiUrl}/cleaning-schedules`, { params: { mine } }).pipe(map(r => r.data));
  }

  createSchedule(request: CleaningScheduleCreateRequest): Observable<CleaningSchedule> {
    return this.http.post<ApiResponse<CleaningSchedule>>(`${this.apiUrl}/cleaning-schedules`, request).pipe(map(r => r.data));
  }

  updateSchedule(id: number, request: Partial<CleaningScheduleCreateRequest> & { status?: string }): Observable<CleaningSchedule> {
    return this.http.put<ApiResponse<CleaningSchedule>>(`${this.apiUrl}/cleaning-schedules/${id}`, request).pipe(map(r => r.data));
  }

  deleteSchedule(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/cleaning-schedules/${id}`).pipe(map(() => void 0));
  }

  listRotations(): Observable<CleaningRotation[]> {
    return this.http.get<ApiResponse<CleaningRotation[]>>(`${this.apiUrl}/cleaning-rotations`).pipe(map(r => r.data));
  }

  createRotation(request: CleaningRotationCreateRequest): Observable<CleaningRotation> {
    return this.http.post<ApiResponse<CleaningRotation>>(`${this.apiUrl}/cleaning-rotations`, request).pipe(map(r => r.data));
  }

  updateRotation(id: number, request: Partial<CleaningRotationCreateRequest> & { active?: boolean }): Observable<CleaningRotation> {
    return this.http.put<ApiResponse<CleaningRotation>>(`${this.apiUrl}/cleaning-rotations/${id}`, request).pipe(map(r => r.data));
  }
}
