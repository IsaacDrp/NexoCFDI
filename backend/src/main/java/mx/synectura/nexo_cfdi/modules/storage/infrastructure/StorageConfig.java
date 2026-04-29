package mx.synectura.nexo_cfdi.modules.storage.infrastructure;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${nexo.storage.endpoint}") String endpoint,
            @Value("${nexo.storage.access-key}") String accessKey,
            @Value("${nexo.storage.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
