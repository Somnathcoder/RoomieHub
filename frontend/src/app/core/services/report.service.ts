import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ReportType = 'monthly-expense' | 'member-contribution' | 'pending-settlement' | 'bill-report';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/reports`;

  download(type: ReportType, format: 'csv' | 'pdf' = 'csv'): Observable<Blob> {
    return this.http.get(`${this.base}/${type}`, { params: { format }, responseType: 'blob' });
  }

  triggerDownload(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
