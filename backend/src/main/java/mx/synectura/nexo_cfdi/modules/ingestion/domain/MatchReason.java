package mx.synectura.nexo_cfdi.modules.ingestion.domain;

public enum MatchReason {
    HAS_XML_PDF,
    HAS_ZIP,
    HAS_XML_ONLY,
    HAS_PDF_ONLY,
    KEYWORD_MATCH,
    KNOWN_INVOICER,
    SENDER_MATCH
}
