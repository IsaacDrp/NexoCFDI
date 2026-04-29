package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.EmailProcessingStatus;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.IngestedEmailSource;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.MatchReason;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "ingested_emails")
@Getter
@Setter
@NoArgsConstructor
public class IngestedEmailEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_account_id")
    private MailAccountEntity mailAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_run_id")
    private IngestionJobRunEntity jobRun;

    @Column(name = "message_id", length = 512)
    private String messageId;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "from_address", length = 320)
    private String fromAddress;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "has_zip", nullable = false)
    private boolean hasZip;

    @Column(name = "has_xml", nullable = false)
    private boolean hasXml;

    @Column(name = "has_pdf", nullable = false)
    private boolean hasPdf;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_reasons", nullable = false, columnDefinition = "jsonb")
    private String matchReasonsJson = "[]";

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private EmailProcessingStatus processingStatus = EmailProcessingStatus.PENDING;

    @Column(name = "error_cause", columnDefinition = "TEXT")
    private String errorCause;

    @Column(name = "cfdi_uuid", length = 36)
    private String cfdiUuid;

    @Column(name = "cfdi_rfc_emisor", length = 13)
    private String cfdiRfcEmisor;

    @Column(name = "cfdi_nombre_emisor", length = 300)
    private String cfdiNombreEmisor;

    @Column(name = "cfdi_fecha")
    private LocalDateTime cfdiFecha;

    @Column(name = "cfdi_subtotal", precision = 15, scale = 2)
    private BigDecimal cfdiSubtotal;

    @Column(name = "cfdi_iva", precision = 15, scale = 2)
    private BigDecimal cfdiIva;

    @Column(name = "cfdi_total", precision = 15, scale = 2)
    private BigDecimal cfdiTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private IngestedEmailSource source = IngestedEmailSource.EMAIL;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "ingestedEmail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IngestedAttachmentEntity> attachments = new ArrayList<>();

    public void setMatchReasons(Set<MatchReason> reasons) {
        try {
            List<String> names = reasons.stream().map(Enum::name).collect(Collectors.toList());
            this.matchReasonsJson = MAPPER.writeValueAsString(names);
        } catch (Exception e) {
            throw new IllegalStateException("Error serializando match_reasons", e);
        }
    }

    public Set<MatchReason> getMatchReasonsAsSet() {
        try {
            if (matchReasonsJson == null || matchReasonsJson.isBlank()) return EnumSet.noneOf(MatchReason.class);
            List<String> names = MAPPER.readValue(matchReasonsJson, new TypeReference<List<String>>() {});
            return names.stream().map(MatchReason::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(MatchReason.class)));
        } catch (Exception e) {
            throw new IllegalStateException("Error deserializando match_reasons", e);
        }
    }
}
