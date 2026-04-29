import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateKeywordRequest, UserKeyword } from '../models/keyword.model';

@Injectable({ providedIn: 'root' })
export class KeywordService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/ingestion/keywords`;

  findAll(): Observable<UserKeyword[]> {
    return this.http.get<UserKeyword[]>(this.base);
  }

  create(request: CreateKeywordRequest): Observable<UserKeyword> {
    return this.http.post<UserKeyword>(this.base, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
