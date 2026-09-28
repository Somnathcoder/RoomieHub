import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

const STORAGE_KEY = 'rms_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private _currentUser = signal<AuthResponse | null>(this.readStored());
  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => !!this._currentUser());
  readonly hasRoom = computed(() => !!this._currentUser()?.roomId);
  readonly isAdmin = computed(() => this._currentUser()?.role === 'ADMIN');
  readonly isModerator = computed(() => this._currentUser()?.role === 'MODERATOR');

  register(request: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/register`, request)
      .pipe(tap(res => this.setSession(res.data)));
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap(res => this.setSession(res.data)));
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${environment.apiUrl}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${environment.apiUrl}/auth/reset-password`, { token, newPassword });
  }

  refreshSession(updated: Partial<AuthResponse>) {
    const current = this._currentUser();
    if (!current) return;
    this.setSession({ ...current, ...updated });
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  get token(): string | null {
    return this._currentUser()?.token ?? null;
  }

  private setSession(auth: AuthResponse): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    this._currentUser.set(auth);
  }

  private readStored(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) as AuthResponse : null;
    } catch {
      return null;
    }
  }
}
