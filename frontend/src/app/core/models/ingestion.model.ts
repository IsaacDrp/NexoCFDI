export type JobTrigger = 'REST' | 'SCHEDULED';
export type JobStatus = 'RUNNING' | 'SUCCESS' | 'FAILED';
export type MatchReason =
  | 'HAS_XML_PDF'
  | 'HAS_ZIP'
  | 'HAS_XML_ONLY'
  | 'HAS_PDF_ONLY'
  | 'KEYWORD_MATCH'
  | 'KNOWN_INVOICER';

export interface IngestionRequest {
  month: number;
  year: number;
}

export interface JobRunResponse {
  id: string;
  triggeredBy: JobTrigger;
  targetYear: number;
  targetMonth: number;
  startedAt: string;
  finishedAt: string | null;
  status: JobStatus;
  accountsTotal: number;
  accountsOk: number;
  accountsFailed: number;
  emailsIngested: number;
  errorMessage: string | null;
}

export interface AttachmentInfo {
  filename: string;
  extension: string;
  sizeBytes: number;
  insideZip: boolean;
  parentZipName: string | null;
  depth: number;
}

export interface IngestedEmailResponse {
  id: string;
  mailAccountId: string;
  messageId: string;
  subject: string | null;
  fromAddress: string | null;
  receivedAt: string;
  hasZip: boolean;
  hasXml: boolean;
  hasPdf: boolean;
  matchReasons: MatchReason[];
  attachments: AttachmentInfo[];
}
