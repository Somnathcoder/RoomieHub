import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Individual feature services (MemberService, ExpenseService, etc.) each
 * expose their own typed upload methods that post to the right endpoint.
 * This service centralizes the handful of cross-cutting file helpers used
 * across the app (resolving a stored relative path to a full, viewable URL).
 */
@Injectable({ providedIn: 'root' })
export class FileUploadService {
  resolveUrl(pathOrUrl: string | null | undefined): string {
    if (!pathOrUrl) return '';
    if (pathOrUrl.startsWith('http')) return pathOrUrl;
    return `${environment.fileBaseUrl}${pathOrUrl}`;
  }
}
