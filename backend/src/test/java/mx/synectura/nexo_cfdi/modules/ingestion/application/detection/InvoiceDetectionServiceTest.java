package mx.synectura.nexo_cfdi.modules.ingestion.application.detection;

import mx.synectura.nexo_cfdi.modules.ingestion.api.RawEmailMessage;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedAttachment;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KeywordType;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.KnownInvoicer;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MatchReason;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.UserKeyword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceDetectionServiceTest {

    private InvoiceDetectionService sut;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sut = new InvoiceDetectionService(
                new ZipScanner(3, 25),
                List.of("factura", "cfdi", "comprobante", "xml")
        );
    }

    // ------------------------------------------------------------------ helpers

    private RawEmailMessage msg(String subject, String from, String body,
                                List<RawEmailMessage.RawAttachment> attachments) {
        return new RawEmailMessage("msg-1", subject, from,
                OffsetDateTime.now(), body, attachments);
    }

    private RawEmailMessage.RawAttachment att(String name) {
        return new RawEmailMessage.RawAttachment(name, ("content-of-" + name).getBytes());
    }

    private RawEmailMessage.RawAttachment att(String name, byte[] content) {
        return new RawEmailMessage.RawAttachment(name, content);
    }

    private byte[] buildZip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                zos.putNextEntry(ze);
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private UserKeyword keyword(String phrase, KeywordType type) {
        return new UserKeyword(UUID.randomUUID(), USER_ID, phrase, type, OffsetDateTime.now());
    }

    private KnownInvoicer invoicer(String emailOrDomain) {
        return new KnownInvoicer(UUID.randomUUID(), USER_ID, emailOrDomain, "label", OffsetDateTime.now());
    }

    // ------------------------------------------------------------------ tests

    @Test
    void xmlAndPdfRootAttachments_isInvoiceWithXmlPdfReason() {
        RawEmailMessage email = msg("Hola", "sender@example.com", "body",
                List.of(att("factura.xml"), att("factura.pdf")));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.hasXml()).isTrue();
        assertThat(result.hasPdf()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.HAS_XML_PDF);
        assertThat(result.reasons()).doesNotContain(MatchReason.HAS_XML_ONLY, MatchReason.HAS_PDF_ONLY);
    }

    @Test
    void xmlOnlyAttachment_isInvoiceWithXmlOnlyReason() {
        RawEmailMessage email = msg("Hola", "sender@example.com", "body",
                List.of(att("factura.xml")));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.hasXml()).isTrue();
        assertThat(result.hasPdf()).isFalse();
        assertThat(result.reasons()).contains(MatchReason.HAS_XML_ONLY);
        assertThat(result.reasons()).doesNotContain(MatchReason.HAS_XML_PDF, MatchReason.HAS_PDF_ONLY);
    }

    @Test
    void pdfOnlyAttachment_isInvoiceWithPdfOnlyReason() {
        RawEmailMessage email = msg("Hola", "sender@example.com", "body",
                List.of(att("factura.pdf")));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.hasPdf()).isTrue();
        assertThat(result.hasXml()).isFalse();
        assertThat(result.reasons()).contains(MatchReason.HAS_PDF_ONLY);
        assertThat(result.reasons()).doesNotContain(MatchReason.HAS_XML_PDF, MatchReason.HAS_XML_ONLY);
    }

    @Test
    void zipContainingXmlAndPdf_hasZipAndXmlPdfReasons() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "factura.xml", "<xml/>".getBytes(),
                "factura.pdf", "%PDF".getBytes()
        ));
        RawEmailMessage email = msg("Hola", "sender@example.com", "body",
                List.of(att("docs.zip", zipBytes)));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.hasZip()).isTrue();
        assertThat(result.hasXml()).isTrue();
        assertThat(result.hasPdf()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.HAS_ZIP, MatchReason.HAS_XML_PDF);
    }

    @Test
    void globalKeywordInSubject_keywordMatchReason() {
        // "Factura" case-insensitive should match global keyword "factura"
        RawEmailMessage email = msg("Factura electronica", "sender@example.com", "body",
                List.of());

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.KEYWORD_MATCH);
    }

    @Test
    void userIncludeKeywordInBody_keywordMatchReason() {
        RawEmailMessage email = msg("Hello", "sender@example.com", "Su nota de cobro adjunta",
                List.of());
        UserKeyword kw = keyword("nota de cobro", KeywordType.INCLUDE);

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of(kw));

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.KEYWORD_MATCH);
    }

    @Test
    void knownInvoicerExactEmailMatch_knownInvoicerReason() {
        RawEmailMessage email = msg("Hola", "billing@acme.com", "body", List.of());
        KnownInvoicer ki = invoicer("billing@acme.com");

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(ki), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.KNOWN_INVOICER);
    }

    @Test
    void knownInvoicerAtDomainSuffixMatch_knownInvoicerReason() {
        // Known invoicer is "acme.com"; from "billing@acme.com" ends with "@acme.com"
        RawEmailMessage email = msg("Hola", "billing@acme.com", "body", List.of());
        KnownInvoicer ki = invoicer("acme.com");

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(ki), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.KNOWN_INVOICER);
    }

    @Test
    void knownInvoicerDotDomainSuffixMatch_knownInvoicerReason() {
        // Known invoicer is "corp.mx"; from "billing@subsidiary.corp.mx" ends with ".corp.mx"
        RawEmailMessage email = msg("Hola", "billing@subsidiary.corp.mx", "body", List.of());
        KnownInvoicer ki = invoicer("corp.mx");

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(ki), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.KNOWN_INVOICER);
    }

    @Test
    void nonMatchingKnownInvoicer_noKnownInvoicerReason() {
        RawEmailMessage email = msg("Hola", "other@unknown.com", "body", List.of());
        KnownInvoicer ki = invoicer("acme.com");

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(ki), List.of());

        assertThat(result.reasons()).doesNotContain(MatchReason.KNOWN_INVOICER);
    }

    @Test
    void senderIncludeKeywordMatchesFromAddress_senderMatchReason() {
        RawEmailMessage email = msg("Hola", "billing@acme.com", "body", List.of());
        UserKeyword kw = keyword("acme.com", KeywordType.SENDER_INCLUDE);

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of(kw));

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.SENDER_MATCH);
    }

    @Test
    void excludeKeywordInSubject_isInvoiceFalseEvenWithXmlPdf() {
        RawEmailMessage email = msg("Este es un borrador", "sender@example.com", "body",
                List.of(att("factura.xml"), att("factura.pdf")));
        UserKeyword excl = keyword("borrador", KeywordType.EXCLUDE);

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of(excl));

        assertThat(result.isInvoice()).isFalse();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void excludeKeywordInBody_isInvoiceFalseEvenWithXmlPdf() {
        RawEmailMessage email = msg("Factura", "sender@example.com", "esto es publicidad",
                List.of(att("factura.xml"), att("factura.pdf")));
        UserKeyword excl = keyword("publicidad", KeywordType.EXCLUDE);

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of(excl));

        assertThat(result.isInvoice()).isFalse();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void senderExcludeMatchingSender_isInvoiceFalseEvenWithAttachments() {
        RawEmailMessage email = msg("Factura", "spam@bad.com", "body",
                List.of(att("factura.xml"), att("factura.pdf")));
        UserKeyword excl = keyword("bad.com", KeywordType.SENDER_EXCLUDE);

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of(excl));

        assertThat(result.isInvoice()).isFalse();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void unrelatedAttachmentNoKeywordsUnknownSender_notInvoice() {
        RawEmailMessage email = msg("Hello team", "random@example.com", "See attachment",
                List.of(att("report.txt")));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isFalse();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void attachments_populatedForEachInputAttachment() {
        RawEmailMessage email = msg("Hola", "sender@example.com", "body",
                List.of(att("factura.xml"), att("factura.pdf")));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.attachments()).hasSize(2);
        assertThat(result.attachments())
                .extracting(IngestedAttachment::extension)
                .containsExactlyInAnyOrder("xml", "pdf");
    }

    @Test
    void zipAttachment_innerFilesAlsoAppearInAttachments() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "factura.xml", "<xml/>".getBytes(),
                "factura.pdf", "%PDF".getBytes()
        ));
        RawEmailMessage email = msg("Hola", "sender@example.com", "body",
                List.of(att("docs.zip", zipBytes)));

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        // 1 zip entry + 2 inner files = 3 total
        assertThat(result.attachments()).hasSize(3);
        assertThat(result.attachments())
                .extracting(IngestedAttachment::extension)
                .contains("zip", "xml", "pdf");
    }

    @Test
    void globalKeywordMatchIsCaseInsensitive() {
        // "CFDI" (uppercase) should match global keyword "cfdi"
        RawEmailMessage email = msg("CFDI recibido", "sender@example.com", "body", List.of());

        InvoiceDetectionService.DetectionResult result = sut.evaluate(email, List.of(), List.of());

        assertThat(result.isInvoice()).isTrue();
        assertThat(result.reasons()).contains(MatchReason.KEYWORD_MATCH);
    }
}
