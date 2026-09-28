import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Room, RoomCreateRequest } from '../models/room.model';

@Injectable({ providedIn: 'root' })
export class RoomService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/rooms`;

  create(request: RoomCreateRequest): Observable<Room> {
    return this.http.post<ApiResponse<Room>>(this.base, request).pipe(map(r => r.data));
  }

  get(id: number): Observable<Room> {
    return this.http.get<ApiResponse<Room>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }
}
