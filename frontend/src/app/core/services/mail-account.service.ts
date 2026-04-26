import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LinkMailRequest, MailAccount } from '../models/mail-account.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MailAccountService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/ingestion`;

  getAccounts(): Observable<MailAccount[]> {
    return this.http.get<MailAccount[]>(`${this.base}/mail-accounts`);
  }

  getAuthUrl(provider: string): Observable<{ authorizationUrl: string }> {
    return this.http.get<{ authorizationUrl: string }>(
      `${this.base}/mail-accounts/auth-url`,
      { params: { provider } },
    );
  }

  linkAccount(request: LinkMailRequest): Observable<MailAccount> {
    return this.http.post<MailAccount>(`${this.base}/mail-accounts/link`, request);
  }

  unlinkAccount(accountId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/mail-accounts/${accountId}`);
  }
}
