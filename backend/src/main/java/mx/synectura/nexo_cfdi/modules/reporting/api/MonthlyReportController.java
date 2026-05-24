package mx.synectura.nexo_cfdi.modules.reporting.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.IngestedEmailEntity;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.IngestedEmailJpaRepository;
import mx.synectura.nexo_cfdi.modules.reporting.application.MonthlyExpenseReportService;
import mx.synectura.nexo_cfdi.modules.reporting.application.ZipArchiveService;
import mx.synectura.nexo_cfdi.modules.reporting.infrastructure.SmtpOAuthMailSender;
import mx.synectura.nexo_cfdi.modules.storage.api.DocumentStoragePort;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserEntity;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserJpaRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class MonthlyReportController {

    private final MonthlyExpenseReportService reportService;
    private final ZipArchiveService zipService;
    private final SmtpOAuthMailSender mailSender;
    private final UserJpaRepository userRepository;
    private final IngestedEmailJpaRepository emailRepository;
    private final DocumentStoragePort storagePort;

    @PostMapping(value = "/monthly/{year}/{month}/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> sendMonthlyReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam String emailDestino,
            @RequestParam String asunto,
            @RequestParam(required = false) String mensajePersonalizado,
            @RequestParam(value = "ingresos", required = false) List<MultipartFile> ingresos) {

        UserEntity user = userRepository.findByMicrosoftSub(jwt.getSubject())
                .orElseThrow(() -> new RuntimeException("User not found"));

        byte[] excelBytes = reportService.generateReport(user, year, month);

        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to = from.plusMonths(1);
        List<IngestedEmailEntity> cfdis = emailRepository.findParsedCfdisByUserAndMonth(user.getId(), from, to);

        byte[] zipBytes = zipService.createZipFromCfdis(cfdis);

        List<SmtpOAuthMailSender.IngresoAttachment> ingresoAttachments = new ArrayList<>();
        if (ingresos != null) {
            for (MultipartFile file : ingresos) {
                try {
                    String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "ingreso.pdf";
                    String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
                    String key = String.format("%d/%02d/%s/ingresos/%s", year, month, user.getRfc(), safeName);
                    byte[] bytes = file.getBytes();
                    storagePort.store(key, bytes, "application/pdf");
                    ingresoAttachments.add(new SmtpOAuthMailSender.IngresoAttachment(safeName, bytes));
                    log.info("Ingreso PDF almacenado key={}", String.format("%d/%02d/%s/ingresos/%s", year, month, user.getRfc(), file.getOriginalFilename()));
                } catch (Exception e) {
                    log.error("Error al procesar ingreso PDF '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new RuntimeException("Error al procesar archivo de ingreso: " + file.getOriginalFilename(), e);
                }
            }
        }

        String fullName = user.getFirstName() + " " + (user.getPaternalSurname() != null ? user.getPaternalSurname() : "");
        String customMsg = mensajePersonalizado != null ? mensajePersonalizado : "";
        String htmlBody = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;\">" +
                          "<h2 style=\"color: #0d6efd;\">Hola,</h2>" +
                          "<p style=\"font-size: 16px; color: #333;\">" + customMsg.replace("\n", "<br>") + "</p>" +
                          "<hr style=\"border: 1px solid #eee; margin: 20px 0;\">" +
                          "<footer style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; text-align: center;\">" +
                          "<p style=\"margin: 0; color: #6c757d; font-size: 14px;\">Atentamente,</p>" +
                          "<p style=\"margin: 5px 0 0 0; font-size: 16px; font-weight: bold; color: #212529;\">" + fullName + "</p>" +
                          "</footer>" +
                          "</div>";

        mailSender.sendEmail(user.getId(), emailDestino, asunto, htmlBody, excelBytes, zipBytes, ingresoAttachments);

        return ResponseEntity.ok(Map.of("message", "Reporte enviado exitosamente"));
    }
}
