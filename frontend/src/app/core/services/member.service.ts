import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Member, MemberCreateRequest, MemberUpdateRequest, Permission, PermissionItem } from '../models/member.model';

@Injectable({ providedIn: 'root' })
export class MemberService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/members`;

  // Nearly every feature page fetches the member list once (for a "who did this" dropdown or
  // name lookup) - without this, switching between pages re-requested the exact same room
  // roster over and over. Shared + replayed per room, invalidated on any mutation below so no
  // page can see stale data.
  private cache$: Observable<Member[]> | null = null;

  list(): Observable<Member[]> {
    if (!this.cache$) {
      this.cache$ = this.http.get<ApiResponse<Member[]>>(this.base).pipe(
        map(r => r.data),
        shareReplay(1)
      );
    }
    return this.cache$;
  }

  private invalidate(): void {
    this.cache$ = null;
  }

  /** Called on logout - this is a singleton service, so its cache would otherwise leak into
   *  whichever different account/room logs in next in the same browser tab. */
  clearCache(): void {
    this.cache$ = null;
  }

  get(id: number): Observable<Member> {
    return this.http.get<ApiResponse<Member>>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  add(request: MemberCreateRequest): Observable<Member> {
    return this.http.post<ApiResponse<Member>>(this.base, request).pipe(map(r => r.data), tap(() => this.invalidate()));
  }

  update(id: number, request: MemberUpdateRequest): Observable<Member> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${id}`, request).pipe(map(r => r.data), tap(() => this.invalidate()));
  }

  changeRole(id: number, role: string): Observable<Member> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${id}/role`, { role }).pipe(map(r => r.data), tap(() => this.invalidate()));
  }

  updateStatus(id: number, status: string): Observable<Member> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${id}/status`, { status }).pipe(map(r => r.data), tap(() => this.invalidate()));
  }

  uploadPhoto(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/photo`, form).pipe(map(r => r.data), tap(() => this.invalidate()));
  }

  uploadIdProof(id: number, file: File): Observable<{ fileUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<{ fileUrl: string }>>(`${this.base}/${id}/id-proof`, form).pipe(map(r => r.data), tap(() => this.invalidate()));
  }

  /**
   * The id-proof endpoint requires a JWT (it's a sensitive document, not served from /uploads/**),
   * so it can't be opened as a plain <a href> - the browser's direct navigation request wouldn't
   * carry the Authorization header and would just render the 401 JSON body. Fetch it as an
   * authenticated blob instead and let the caller open/download that.
   */
  viewIdProof(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/id-proof`, { responseType: 'blob' });
  }

  getPermissions(id: number): Observable<Permission[]> {
    return this.http.get<ApiResponse<Permission[]>>(`${this.base}/${id}/permissions`).pipe(map(r => r.data));
  }

  updatePermissions(id: number, permissions: PermissionItem[]): Observable<Permission[]> {
    return this.http.put<ApiResponse<Permission[]>>(`${this.base}/${id}/permissions`, { permissions }).pipe(map(r => r.data));
  }
}
