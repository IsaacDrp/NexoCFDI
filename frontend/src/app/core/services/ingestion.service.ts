import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  IngestedEmailResponse,
  IngestionRequest,
  JobRunResponse,
  PresignedUrlResponse,
} from '../models/ingestion.model';

@Injectable({ providedIn: 'root' })
export class IngestionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/ingestion`;

  triggerIngestion(request: IngestionRequest): Observable<JobRunResponse> {
    return this.http.post<JobRunResponse>(`${this.base}/search`, request);
  }

  getJobStatus(jobId: string): Observable<JobRunResponse> {
    return this.http.get<JobRunResponse>(`${this.base}/jobs/${jobId}`);
  }

  findEmails(year: number, month: number): Observable<IngestedEmailResponse[]> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<IngestedEmailResponse[]>(`${this.base}/emails`, { params });
  }

  addManualEntry(formData: FormData): Observable<IngestedEmailResponse> {
    return this.http.post<IngestedEmailResponse>(`${this.base}/emails/manual`, formData);
  }

  updateEmail(id: string, formData: FormData): Observable<IngestedEmailResponse> {
    return this.http.put<IngestedEmailResponse>(`${this.base}/emails/${id}`, formData);
  }

  getAttachmentPreviewUrl(emailId: string, attachmentId: string): Observable<PresignedUrlResponse> {
    return this.http.get<PresignedUrlResponse>(`${this.base}/emails/${emailId}/attachments/${attachmentId}/preview`);
  }
}
