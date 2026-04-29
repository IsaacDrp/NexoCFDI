package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.imap;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.api.EmailReaderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.api.RawEmailMessage;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.encryption.AesGcmTokenEncryptionService;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth.OAuthProviderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth.OAuthTokens;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.MailAccountEntity;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.MailAccountJpaRepository;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImapMailReaderAdapter implements EmailReaderPort {

    private static final String IMAP_HOST = "outlook.office365.com";
    private static final int    IMAP_PORT = 993;

    private final MailAccountJpaRepository mailAccountJpa;
    private final AesGcmTokenEncryptionService encryptionService;
    private final Map<String, OAuthProviderPort> oauthProviders;

    @Override
    public List<RawEmailMessage> searchInbox(UUID mailAccountId, OffsetDateTime from, OffsetDateTime to) {
        MailAccountEntity account = mailAccountJpa.findById(mailAccountId)
                .orElseThrow(() -> new IllegalArgumentException("MailAccount no encontrado: " + mailAccountId));

        String email = account.getEmailAddress();
        log.info("IMAP_CONNECT cuenta={} email={} host={}:{}", mailAccountId, email, IMAP_HOST, IMAP_PORT);

        String accessToken = obtainAccessToken(account);
        log.debug("IMAP_TOKEN_OK cuenta={} email={}", mailAccountId, email);

        Session session = buildSession();

        try (Store store = session.getStore("imap")) {
            store.connect(IMAP_HOST, IMAP_PORT, email, accessToken);
            log.info("IMAP_CONNECTED cuenta={} email={}", mailAccountId, email);

            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_ONLY);
                log.debug("IMAP_INBOX_OPEN cuenta={} total_mensajes={}", mailAccountId, inbox.getMessageCount());

                SearchTerm term = buildDateRangeTerm(from, to);
                log.info("IMAP_SEARCH cuenta={} email={} desde={} hasta={}", mailAccountId, email, from, to);

                Message[] messages = inbox.search(term);
                log.info("IMAP_SEARCH_RESULT cuenta={} email={} encontrados={}", mailAccountId, email, messages.length);

                List<RawEmailMessage> result = new ArrayList<>(messages.length);
                int errores = 0;
                for (Message msg : messages) {
                    try {
                        RawEmailMessage raw = toRaw(msg);
                        log.debug("IMAP_MSG cuenta={} msgId={} from={} subject=\"{}\" adjuntos={}",
                                mailAccountId, raw.messageId(), raw.fromAddress(), raw.subject(),
                                raw.attachments().size());
                        if (!raw.attachments().isEmpty()) {
                            raw.attachments().forEach(att ->
                                    log.debug("IMAP_ADJUNTO cuenta={} msgId={} archivo={} bytes={}",
                                            mailAccountId, raw.messageId(), att.filename(), att.content().length));
                        }
                        result.add(raw);
                    } catch (Exception ex) {
                        errores++;
                        log.warn("IMAP_MSG_ERROR cuenta={} email={} error={}", mailAccountId, email, ex.getMessage());
                    }
                }

                if (errores > 0) {
                    log.warn("IMAP_PARSE_ERRORS cuenta={} email={} errores={} de {}",
                            mailAccountId, email, errores, messages.length);
                }
                log.info("IMAP_DONE cuenta={} email={} procesados={} errores={}",
                        mailAccountId, email, result.size(), errores);
                return result;
            }
        } catch (MessagingException e) {
            log.error("IMAP_FAIL cuenta={} email={} causa={}", mailAccountId, email, e.getMessage(), e);
            throw new ImapAccessException("Error accediendo IMAP de " + email, e);
        }
    }

    private String obtainAccessToken(MailAccountEntity account) {
        OAuthProviderPort provider = oauthProviders.get(account.getProvider().name());
        if (provider == null) {
            throw new IllegalStateException("Proveedor OAuth no soportado: " + account.getProvider());
        }
        String encryptedRefresh = account.getEncryptedRefreshToken() + ":" + account.getTokenIv();
        OAuthTokens tokens = provider.refreshAccessToken(encryptedRefresh);

        String newRefresh   = tokens.refreshToken();
        String plainOldRefresh = encryptionService.decrypt(account.getEncryptedRefreshToken(), account.getTokenIv());
        if (newRefresh != null && !newRefresh.equals(plainOldRefresh)) {
            log.debug("OAUTH_TOKEN_ROTADO cuenta={} email={}", account.getId(), account.getEmailAddress());
            AesGcmTokenEncryptionService.EncryptedToken updated = encryptionService.encrypt(newRefresh);
            account.setEncryptedRefreshToken(updated.ciphertext());
            account.setTokenIv(updated.iv());
            mailAccountJpa.save(account);
        }
        return tokens.accessToken();
    }

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", IMAP_HOST);
        props.put("mail.imap.port", String.valueOf(IMAP_PORT));
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.imap.auth.mechanisms", "XOAUTH2");
        props.put("mail.imap.auth.login.disable", "true");
        props.put("mail.imap.auth.plain.disable", "true");
        props.put("mail.imap.starttls.enable", "false");
        props.put("mail.debug", "false");
        return Session.getInstance(props);
    }

    private SearchTerm buildDateRangeTerm(OffsetDateTime from, OffsetDateTime to) {
        Date fromDate = Date.from(from.toInstant());
        Date toDate   = Date.from(to.toInstant());
        return new AndTerm(
                new ReceivedDateTerm(ComparisonTerm.GE, fromDate),
                new ReceivedDateTerm(ComparisonTerm.LT, toDate));
    }

    private RawEmailMessage toRaw(Message msg) throws MessagingException, IOException {
        String messageId  = extractMessageId(msg);
        String subject    = msg.getSubject();
        String fromAddress = extractFrom(msg);
        OffsetDateTime receivedAt = msg.getReceivedDate() != null
                ? msg.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                : OffsetDateTime.now();

        StringBuilder bodyText  = new StringBuilder();
        List<RawEmailMessage.RawAttachment> attachments = new ArrayList<>();
        collectParts(msg, bodyText, attachments);

        return new RawEmailMessage(messageId, subject, fromAddress, receivedAt, bodyText.toString(), attachments);
    }

    private String extractMessageId(Message msg) throws MessagingException {
        String[] ids = msg.getHeader("Message-ID");
        if (ids != null && ids.length > 0) return ids[0];
        return "msg-" + msg.getSentDate() + "-" + msg.getSubject();
    }

    private String extractFrom(Message msg) throws MessagingException {
        Address[] froms = msg.getFrom();
        if (froms == null || froms.length == 0) return null;
        Address first = froms[0];
        if (first instanceof InternetAddress ia) return ia.getAddress();
        return first.toString();
    }

    private void collectParts(Part part, StringBuilder bodyText,
                              List<RawEmailMessage.RawAttachment> attachments) throws MessagingException, IOException {
        Object content = part.getContent();
        if (content instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                collectParts(mp.getBodyPart(i), bodyText, attachments);
            }
            return;
        }

        String disposition = part.getDisposition();
        String filename    = part.getFileName();
        boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || Part.INLINE.equalsIgnoreCase(disposition)
                || (filename != null && !filename.isBlank());

        if (isAttachment && filename != null) {
            byte[] bytes = readBytes(part);
            attachments.add(new RawEmailMessage.RawAttachment(MimeUtilityHelper.decode(filename), bytes));
            return;
        }

        if (part.isMimeType("text/plain") && content instanceof String s) {
            bodyText.append(s).append('\n');
        } else if (part.isMimeType("text/html") && content instanceof String s) {
            bodyText.append(s).append('\n');
        }
    }

    private byte[] readBytes(Part part) throws IOException, MessagingException {
        try (InputStream in = part.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    public static class ImapAccessException extends RuntimeException {
        public ImapAccessException(String message, Throwable cause) { super(message, cause); }
    }
}
