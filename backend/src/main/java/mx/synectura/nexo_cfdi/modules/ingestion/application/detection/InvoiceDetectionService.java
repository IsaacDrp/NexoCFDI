package mx.synectura.nexo_cfdi.modules.ingestion.application.detection;

import mx.synectura.nexo_cfdi.modules.ingestion.api.RawEmailMessage;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedAttachment;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KeywordType;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KnownInvoicer;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MatchReason;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeyword;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Aplica las 5 reglas OR para decidir si un correo se considera factura:
 *  1) Tiene .zip (con contenido válido tras escaneo)
 *  2) Tiene la dupla XML + PDF (en raíz o dentro de zip)
 *  3) Tiene XML solo o PDF solo
 *  4) Asunto/cuerpo contiene palabras clave globales o del usuario (INCLUDE)
 *  5) Remitente está en catálogo `known_invoicers` del usuario
 *
 * Si el asunto/cuerpo coincide con alguna keyword EXCLUDE del usuario, el correo
 * se descarta aunque cumpla otras reglas.
 */
@Service
public class InvoiceDetectionService {

    private final ZipScanner zipScanner;
    private final List<String> globalKeywords;

    public InvoiceDetectionService(ZipScanner zipScanner,
                                   @Value("${nexo.ingestion.invoice-keywords}") List<String> invoiceKeywords) {
        this.zipScanner = zipScanner;
        this.globalKeywords = invoiceKeywords.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
    }

    public DetectionResult evaluate(RawEmailMessage msg,
                                    List<KnownInvoicer> knownInvoicers,
                                    List<UserKeyword> userKeywords) {
        String haystack = buildHaystack(msg);

        if (matchesSenderExclude(msg.fromAddress(), userKeywords)) {
            return new DetectionResult(false, EnumSet.noneOf(MatchReason.class),
                    false, false, false, List.of());
        }

        if (matchesExclude(haystack, userKeywords)) {
            return new DetectionResult(false, EnumSet.noneOf(MatchReason.class),
                    false, false, false, List.of());
        }

        Set<MatchReason> reasons = EnumSet.noneOf(MatchReason.class);
        List<IngestedAttachment> attachments = new ArrayList<>();

        boolean hasZip = false;
        boolean hasXml = false;
        boolean hasPdf = false;

        for (RawEmailMessage.RawAttachment att : msg.attachments()) {
            String filename = att.filename();
            String ext = ZipScanner.extensionOf(filename);

            if ("zip".equals(ext)) {
                hasZip = true;
                attachments.add(new IngestedAttachment(null, null, filename, "zip",
                        att.content().length, false, null, 0, null));
                List<ZipScanner.ScannedFile> inner = zipScanner.scan(filename, att.content());
                for (ZipScanner.ScannedFile sf : inner) {
                    if ("xml".equals(sf.extension())) hasXml = true;
                    if ("pdf".equals(sf.extension())) hasPdf = true;
                    attachments.add(new IngestedAttachment(null, null, sf.filename(), sf.extension(),
                            sf.sizeBytes(), sf.insideZip(), sf.parentZipName(), sf.depth(), null));
                }
            } else {
                if ("xml".equals(ext)) hasXml = true;
                if ("pdf".equals(ext)) hasPdf = true;
                attachments.add(new IngestedAttachment(null, null, filename, ext,
                        att.content().length, false, null, 0, null));
            }
        }

        if (hasZip) reasons.add(MatchReason.HAS_ZIP);
        if (hasXml && hasPdf) reasons.add(MatchReason.HAS_XML_PDF);
        else if (hasXml) reasons.add(MatchReason.HAS_XML_ONLY);
        else if (hasPdf) reasons.add(MatchReason.HAS_PDF_ONLY);

        if (matchesInclude(haystack, userKeywords)) reasons.add(MatchReason.KEYWORD_MATCH);
        if (matchesKnownInvoicer(msg.fromAddress(), knownInvoicers)) reasons.add(MatchReason.KNOWN_INVOICER);
        if (matchesSenderInclude(msg.fromAddress(), userKeywords)) reasons.add(MatchReason.SENDER_MATCH);

        boolean isInvoice = !reasons.isEmpty();
        return new DetectionResult(isInvoice, reasons, hasZip, hasXml, hasPdf, attachments);
    }

    private String buildHaystack(RawEmailMessage msg) {
        return (nullSafe(msg.subject()) + " " + nullSafe(msg.bodyText())).toLowerCase(Locale.ROOT);
    }

    private boolean matchesInclude(String haystack, List<UserKeyword> userKeywords) {
        for (String kw : globalKeywords) {
            if (haystack.contains(kw)) return true;
        }
        if (userKeywords != null) {
            for (UserKeyword kw : userKeywords) {
                if (kw.type() == KeywordType.INCLUDE &&
                        haystack.contains(kw.phrase().toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    private boolean matchesExclude(String haystack, List<UserKeyword> userKeywords) {
        if (userKeywords == null) return false;
        for (UserKeyword kw : userKeywords) {
            if (kw.type() == KeywordType.EXCLUDE &&
                    haystack.contains(kw.phrase().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private boolean matchesKnownInvoicer(String fromAddress, List<KnownInvoicer> known) {
        if (fromAddress == null || known == null || known.isEmpty()) return false;
        String from = fromAddress.toLowerCase(Locale.ROOT).trim();
        for (KnownInvoicer ki : known) {
            if (matchesSender(from, ki.emailOrDomain())) return true;
        }
        return false;
    }

    private boolean matchesSenderExclude(String fromAddress, List<UserKeyword> userKeywords) {
        if (fromAddress == null || userKeywords == null) return false;
        String from = fromAddress.toLowerCase(Locale.ROOT).trim();
        for (UserKeyword kw : userKeywords) {
            if (kw.type() == KeywordType.SENDER_EXCLUDE && matchesSender(from, kw.phrase())) return true;
        }
        return false;
    }

    private boolean matchesSenderInclude(String fromAddress, List<UserKeyword> userKeywords) {
        if (fromAddress == null || userKeywords == null) return false;
        String from = fromAddress.toLowerCase(Locale.ROOT).trim();
        for (UserKeyword kw : userKeywords) {
            if (kw.type() == KeywordType.SENDER_INCLUDE && matchesSender(from, kw.phrase())) return true;
        }
        return false;
    }

    private boolean matchesSender(String from, String filter) {
        String f = filter.toLowerCase(Locale.ROOT).trim();
        return from.equals(f) || from.endsWith("@" + f) || from.endsWith("." + f);
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

    public record DetectionResult(
            boolean isInvoice,
            Set<MatchReason> reasons,
            boolean hasZip,
            boolean hasXml,
            boolean hasPdf,
            List<IngestedAttachment> attachments
    ) {
        public List<IngestedAttachment> attachmentsFor(UUID ingestedEmailId) {
            return attachments.stream()
                    .map(a -> new IngestedAttachment(null, ingestedEmailId, a.filename(), a.extension(),
                            a.sizeBytes(), a.insideZip(), a.parentZipName(), a.depth(), null))
                    .toList();
        }
    }
}
