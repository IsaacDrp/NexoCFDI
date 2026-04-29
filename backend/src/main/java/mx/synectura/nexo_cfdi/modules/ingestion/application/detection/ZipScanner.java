package mx.synectura.nexo_cfdi.modules.ingestion.application.detection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Recorre ZIPs (recursivo hasta `maxDepth`) y devuelve la lista plana de archivos
 * encontrados con metadata de profundidad y zip padre. No persiste contenido.
 */
@Component
@Slf4j
public class ZipScanner {

    private final int maxDepth;
    private final long maxUncompressedBytes;

    public ZipScanner(@Value("${nexo.ingestion.zip.max-depth:3}") int maxDepth,
                      @Value("${nexo.ingestion.zip.max-size-mb:25}") int maxSizeMb) {
        this.maxDepth = maxDepth;
        this.maxUncompressedBytes = (long) maxSizeMb * 1024L * 1024L;
    }

    public List<ScannedFile> scan(String zipFilename, byte[] zipBytes) {
        List<ScannedFile> result = new ArrayList<>();
        scanInternal(zipFilename, zipBytes, 1, result, 0L);
        return result;
    }

    /** Como {@link #scan} pero incluye el contenido binario de cada archivo. */
    public List<ExtractedFile> extract(String zipFilename, byte[] zipBytes) {
        List<ExtractedFile> result = new ArrayList<>();
        extractInternal(zipFilename, zipBytes, 1, result, 0L);
        return result;
    }

    private long extractInternal(String parentZipName, byte[] zipBytes, int depth,
                                 List<ExtractedFile> result, long accumulatedBytes) {
        if (depth > maxDepth) {
            log.warn("Profundidad máxima de zip alcanzada ({}), omitiendo {}", maxDepth, parentZipName);
            return accumulatedBytes;
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                byte[] entryBytes = readEntry(zis);
                accumulatedBytes += entryBytes.length;
                if (accumulatedBytes > maxUncompressedBytes) {
                    log.warn("Tamaño descomprimido excede límite ({} bytes) para {}, abortando", maxUncompressedBytes, parentZipName);
                    return accumulatedBytes;
                }
                String entryName = entry.getName();
                if (isZip(entryName)) {
                    accumulatedBytes = extractInternal(entryName, entryBytes, depth + 1, result, accumulatedBytes);
                } else {
                    result.add(new ExtractedFile(entryName, extensionOf(entryName),
                            entryBytes, true, parentZipName, depth));
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo zip {}: {}", parentZipName, e.getMessage());
        }
        return accumulatedBytes;
    }

    private long scanInternal(String parentZipName, byte[] zipBytes, int depth,
                              List<ScannedFile> result, long accumulatedBytes) {
        if (depth > maxDepth) {
            log.warn("Profundidad máxima de zip alcanzada ({}), omitiendo {}", maxDepth, parentZipName);
            return accumulatedBytes;
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                byte[] entryBytes = readEntry(zis);
                accumulatedBytes += entryBytes.length;
                if (accumulatedBytes > maxUncompressedBytes) {
                    log.warn("Tamaño descomprimido excede límite ({} bytes) para {}, abortando", maxUncompressedBytes, parentZipName);
                    return accumulatedBytes;
                }
                String entryName = entry.getName();
                if (isZip(entryName)) {
                    accumulatedBytes = scanInternal(entryName, entryBytes, depth + 1, result, accumulatedBytes);
                } else {
                    result.add(new ScannedFile(entryName, extensionOf(entryName),
                            entryBytes.length, true, parentZipName, depth));
                }
            }
        } catch (Exception e) {
            log.warn("Error escaneando zip {}: {}", parentZipName, e.getMessage());
        }
        return accumulatedBytes;
    }

    private byte[] readEntry(ZipInputStream zis) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = zis.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > maxUncompressedBytes) break;
        }
        return out.toByteArray();
    }

    public static boolean isZip(String name) {
        return name != null && name.toLowerCase().endsWith(".zip");
    }

    public static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0) return "";
        return name.substring(dot + 1).toLowerCase();
    }

    public record ScannedFile(
            String filename,
            String extension,
            long sizeBytes,
            boolean insideZip,
            String parentZipName,
            int depth
    ) {}

    public record ExtractedFile(
            String filename,
            String extension,
            byte[] content,
            boolean insideZip,
            String parentZipName,
            int depth
    ) {}
}
