package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.imap;

import jakarta.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;

final class MimeUtilityHelper {
    private MimeUtilityHelper() {}

    /** Decodifica nombres de archivo con encoding MIME (RFC 2047) si aplica. */
    static String decode(String raw) {
        if (raw == null) return null;
        try {
            return MimeUtility.decodeText(raw);
        } catch (UnsupportedEncodingException e) {
            return raw;
        }
    }
}
