import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { InventoryItem, InventoryItemCreateRequest } from '../models/inventory.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/inventory`;

  list(): Observable<InventoryItem[]> {
    return this.http.get<ApiResponse<InventoryItem[]>>(this.base).pipe(map(r => r.data));
  }

  create(request: InventoryItemCreateRequest): Observable<InventoryItem> {
    return this.http.post<ApiResponse<InventoryItem>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: Partial<InventoryItemCreateRequest>): Observable<InventoryItem> {
    return this.http.put<ApiResponse<InventoryItem>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`).pipe(map(() => void 0));
  }

  uploadPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/photo`, form).pipe(map(r => r.data));
  }
}
