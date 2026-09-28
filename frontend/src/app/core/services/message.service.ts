import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Message } from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/messages`;

  getPublic(): Observable<Message[]> {
    return this.http.get<ApiResponse<Message[]>>(`${this.base}/public`).pipe(map(r => r.data));
  }

  sendPublic(content: string): Observable<Message> {
    return this.http.post<ApiResponse<Message>>(`${this.base}/public`, { content }).pipe(map(r => r.data));
  }

  getConversation(otherUserId: number): Observable<Message[]> {
    return this.http.get<ApiResponse<Message[]>>(`${this.base}/private/${otherUserId}`).pipe(map(r => r.data));
  }

  sendPrivate(receiverUserId: number, content: string): Observable<Message> {
    return this.http.post<ApiResponse<Message>>(`${this.base}/private`, { receiverUserId, content }).pipe(map(r => r.data));
  }
}
