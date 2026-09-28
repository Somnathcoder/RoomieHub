import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Member, MemberCreateRequest, MemberUpdateRequest, Permission, PermissionItem } from '../models/member.model';

@Injectable({ providedIn: 'root' })
export class MemberService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/members`;

  list(): Observable<Member[]> {
    return this.http.get<ApiResponse<Member[]>>(this.base).pipe(map(r => r.data));
  }

  get(id: number): Observable<Member> {
    return this.http.get<ApiResponse<Member>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  add(request: MemberCreateRequest): Observable<Member> {
    return this.http.post<ApiResponse<Member>>(this.base, request).pipe(map(r => r.data));
  }

  update(id: number, request: MemberUpdateRequest): Observable<Member> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${id}`, request).pipe(map(r => r.data));
  }

  changeRole(id: number, role: string): Observable<Member> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${id}/role`, { role }).pipe(map(r => r.data));
  }

  updateStatus(id: number, status: string): Observable<Member> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${id}/status`, { status }).pipe(map(r => r.data));
  }

  deactivate(id: number): Observable<Member> {
    return this.http.delete<ApiResponse<Member>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  uploadPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/photo`, form).pipe(map(r => r.data));
  }

  uploadIdProof(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/id-proof`, form).pipe(map(r => r.data));
  }

  idProofDownloadUrl(id: number): string {
    return `${this.base}/${id}/id-proof`;
  }

  getPermissions(id: number): Observable<Permission[]> {
    return this.http.get<ApiResponse<Permission[]>>(`${this.base}/${id}/permissions`).pipe(map(r => r.data));
  }

  updatePermissions(id: number, permissions: PermissionItem[]): Observable<Permission[]> {
    return this.http.put<ApiResponse<Permission[]>>(`${this.base}/${id}/permissions`, { permissions }).pipe(map(r => r.data));
  }
}
