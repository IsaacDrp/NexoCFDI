import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SendMonthlyReportRequest {
  emailDestino: string;
  asunto: string;
  mensajePersonalizado: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReportingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/reports`;

  sendMonthlyReport(year: number, month: number, request: SendMonthlyReportRequest, ingresos: File[] = []): Observable<void> {
    const fd = new FormData();
    fd.append('emailDestino', request.emailDestino);
    fd.append('asunto', request.asunto);
    if (request.mensajePersonalizado) fd.append('mensajePersonalizado', request.mensajePersonalizado);
    ingresos.forEach(f => fd.append('ingresos', f, f.name));
    return this.http.post<void>(`${this.base}/monthly/${year}/${month}/send`, fd);
  }
}
