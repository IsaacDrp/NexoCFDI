export type KeywordType = 'INCLUDE' | 'EXCLUDE' | 'SENDER_INCLUDE' | 'SENDER_EXCLUDE';

export interface UserKeyword {
  id: string;
  phrase: string;
  type: KeywordType;
  createdAt: string;
}

export interface CreateKeywordRequest {
  phrase: string;
  type: KeywordType;
}
