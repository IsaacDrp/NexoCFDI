export type MailProvider = 'MICROSOFT' | 'GOOGLE';
export type SyncStatus = 'ACTIVE' | 'PAUSED' | 'REVOKED' | 'ERROR';

export interface MailAccount {
  id: string;
  displayName: string;
  emailAddress: string;
  provider: MailProvider;
  status: SyncStatus;
  lastSyncAt: string | null;
  createdAt: string;
}

export interface LinkMailRequest {
  authorizationCode: string;
  redirectUri: string;
  displayName: string;
  provider: MailProvider;
}
