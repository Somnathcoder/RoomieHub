import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Task, TaskCreateRequest } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/tasks`;

  list(mine = false): Observable<Task[]> {
    return this.http.get<ApiResponse<Task[]>>(this.base, { params: { mine } }).pipe(map(r => r.data));
  }

  create(request: TaskCreateRequest): Observable<Task> {
    return this.http.post<ApiResponse<Task>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: Partial<TaskCreateRequest> & { status?: string }): Observable<Task> {
    return this.http.put<ApiResponse<Task>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }
}
