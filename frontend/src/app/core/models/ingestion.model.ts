export type JobTrigger = 'REST' | 'SCHEDULED';
export type JobStatus = 'RUNNING' | 'SUCCESS' | 'FAILED';
export type MatchReason =
  | 'HAS_XML_PDF'
  | 'HAS_ZIP'
  | 'HAS_XML_ONLY'
  | 'HAS_PDF_ONLY'
  | 'KEYWORD_MATCH'
  | 'KNOWN_INVOICER'
  | 'SENDER_MATCH';

export type EmailProcessingStatus = 'PENDING' | 'STORED' | 'ERROR';
export type IngestedEmailSource = 'EMAIL' | 'MANUAL';

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
  id: string;
  filename: string;
  extension: string;
  sizeBytes: number;
  insideZip: boolean;
  parentZipName: string | null;
  depth: number;
  storageKey: string | null;
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
  processingStatus: EmailProcessingStatus;
  errorCause: string | null;
  cfdiUuid: string | null;
  cfdiRfcEmisor: string | null;
  cfdiNombreEmisor: string | null;
  cfdiFecha: string | null;
  cfdiSubtotal: number | null;
  cfdiIva: number | null;
  cfdiTotal: number | null;
  source: IngestedEmailSource;
}

export interface UpdateEmailRequest {
  cfdiUuid?: string;
  rfcEmisor?: string;
  nombreEmisor?: string;
  fecha?: string;
  subtotal?: number | null;
  iva?: number | null;
  total?: number | null;
}

export interface PresignedUrlResponse {
  url: string;
}

