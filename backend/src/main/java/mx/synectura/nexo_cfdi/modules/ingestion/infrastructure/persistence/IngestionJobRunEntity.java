package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobStatus;
import mx.synectura.nexo_cfdi.modules.ingestion.domain.JobTrigger;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingestion_job_runs")
@Getter
@Setter
@NoArgsConstructor
public class IngestionJobRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "triggered_by", nullable = false)
    private JobTrigger triggeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    private UserEntity triggeredByUser;

    @Column(name = "target_year", nullable = false)
    private int targetYear;

    @Column(name = "target_month", nullable = false)
    private int targetMonth;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "accounts_total", nullable = false)
    private int accountsTotal;

    @Column(name = "accounts_ok", nullable = false)
    private int accountsOk;

    @Column(name = "accounts_failed", nullable = false)
    private int accountsFailed;

    @Column(name = "emails_ingested", nullable = false)
    private int emailsIngested;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
