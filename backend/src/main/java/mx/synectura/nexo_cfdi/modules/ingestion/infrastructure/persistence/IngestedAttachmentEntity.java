package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingested_attachments")
@Getter
@Setter
@NoArgsConstructor
public class IngestedAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingested_email_id", nullable = false)
    private IngestedEmailEntity ingestedEmail;

    @Column(name = "filename", nullable = false, length = 512)
    private String filename;

    @Column(name = "extension", nullable = false, length = 20)
    private String extension;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "inside_zip", nullable = false)
    private boolean insideZip;

    @Column(name = "parent_zip_name", length = 512)
    private String parentZipName;

    @Column(name = "depth", nullable = false)
    private short depth;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
