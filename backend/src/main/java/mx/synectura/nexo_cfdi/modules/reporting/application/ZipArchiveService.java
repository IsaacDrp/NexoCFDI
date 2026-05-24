package mx.synectura.nexo_cfdi.modules.reporting.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.IngestedAttachmentEntity;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.IngestedEmailEntity;
import mx.synectura.nexo_cfdi.modules.storage.api.DocumentStoragePort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipArchiveService {

    private final DocumentStoragePort storagePort;

    public byte[] createZipFromCfdis(List<IngestedEmailEntity> cfdis) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (IngestedEmailEntity cfdi : cfdis) {
                if (cfdi.getAttachments() == null) continue;
                for (IngestedAttachmentEntity attachment : cfdi.getAttachments()) {
                    if (attachment.getStorageKey() == null) continue;
                    
                    String ext = attachment.getExtension().toLowerCase();
                    if (ext.endsWith("xml") || ext.endsWith("pdf")) {
                        try {
                            byte[] data = storagePort.fetch(attachment.getStorageKey());
                            ZipEntry entry = new ZipEntry(attachment.getFilename());
                            zos.putNextEntry(entry);
                            zos.write(data);
                            zos.closeEntry();
                        } catch (Exception e) {
                            log.error("Could not fetch or zip attachment {} for cfdi {}", attachment.getStorageKey(), cfdi.getId(), e);
                        }
                    }
                }
            }

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error creating zip archive", e);
            throw new RuntimeException("Error creating zip archive", e);
        }
    }
}
