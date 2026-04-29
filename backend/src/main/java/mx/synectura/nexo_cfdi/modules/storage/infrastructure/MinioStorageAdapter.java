package mx.synectura.nexo_cfdi.modules.storage.infrastructure;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.storage.api.DocumentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@Slf4j
public class MinioStorageAdapter implements DocumentStoragePort {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageAdapter(MinioClient minioClient,
                               @Value("${nexo.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public String store(String objectKey, byte[] content, String contentType) {
        log.debug("MINIO_PUT_INICIO bucket={} key={} bytes={} contentType={}",
                bucket, objectKey, content.length, contentType);
        long t0 = System.currentTimeMillis();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("MINIO_PUT_OK bucket={} key={} bytes={} ms={}",
                    bucket, objectKey, content.length, System.currentTimeMillis() - t0);
            return objectKey;
        } catch (Exception e) {
            log.error("MINIO_PUT_FAIL bucket={} key={} bytes={} ms={} causa={}",
                    bucket, objectKey, content.length, System.currentTimeMillis() - t0,
                    e.getMessage(), e);
            throw new RuntimeException("Error al subir objeto a MinIO: " + objectKey, e);
        }
    }

    @Override
    public void remove(String objectKey) {
        log.debug("MINIO_DELETE_INICIO bucket={} key={}", bucket, objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("MINIO_DELETE_OK bucket={} key={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("MINIO_DELETE_FAIL bucket={} key={} causa={}", bucket, objectKey, e.getMessage(), e);
            throw new RuntimeException("Error al eliminar objeto de MinIO: " + objectKey, e);
        }
    }
}
