package mx.synectura.nexo_cfdi.modules.reporting.infrastructure;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MailProvider;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.encryption.AesGcmTokenEncryptionService;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth.OAuthProviderPort;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.oauth.OAuthTokens;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.MailAccountEntity;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.MailAccountJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpOAuthMailSender {

    public record IngresoAttachment(String filename, byte[] bytes) {}

    private final MailAccountJpaRepository mailAccountRepository;
    private final AesGcmTokenEncryptionService encryptionService;
    private final List<OAuthProviderPort> oauthProviders;

    public void sendEmail(UUID userId, String to, String subject, String body,
                          byte[] excelBytes, byte[] zipBytes,
                          List<IngresoAttachment> ingresos) {
        List<MailAccountEntity> accounts = mailAccountRepository.findAllByUserId(userId);
        if (accounts.isEmpty()) {
            throw new RuntimeException("El usuario no tiene cuentas de correo configuradas.");
        }
        
        MailAccountEntity account = accounts.get(0);
        String encryptedRefresh = account.getEncryptedRefreshToken() + ":" + account.getTokenIv();
        
        OAuthProviderPort providerPort = oauthProviders.stream()
                .filter(p -> p.getProvider() == account.getProvider())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No provider found for " + account.getProvider()));
                
        OAuthTokens tokens = providerPort.refreshAccessToken(encryptedRefresh);
        String accessToken = tokens.accessToken();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        
        String host = account.getProvider() == MailProvider.MICROSOFT ? "smtp.office365.com" : "smtp.gmail.com";
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(account.getEmailAddress()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            MimeBodyPart excelPart = new MimeBodyPart();
            excelPart.setDataHandler(new jakarta.activation.DataHandler(new ByteArrayDataSource(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
            excelPart.setFileName("reporte_gastos.xlsx");
            multipart.addBodyPart(excelPart);

            MimeBodyPart zipPart = new MimeBodyPart();
            zipPart.setDataHandler(new jakarta.activation.DataHandler(new ByteArrayDataSource(zipBytes, "application/zip")));
            zipPart.setFileName("gastos_mes.zip");
            multipart.addBodyPart(zipPart);

            for (IngresoAttachment ingreso : ingresos) {
                MimeBodyPart ingresoPart = new MimeBodyPart();
                ingresoPart.setDataHandler(new jakarta.activation.DataHandler(new ByteArrayDataSource(ingreso.bytes(), "application/pdf")));
                ingresoPart.setFileName(ingreso.filename());
                multipart.addBodyPart(ingresoPart);
            }

            msg.setContent(multipart);

            Transport transport = session.getTransport("smtp");
            transport.connect(host, account.getEmailAddress(), accessToken);
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
            
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
