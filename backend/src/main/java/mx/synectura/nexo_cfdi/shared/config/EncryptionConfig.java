package mx.synectura.nexo_cfdi.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class EncryptionConfig {

    @Value("${nexo.encryption.key}")
    private String base64Key;

    @Bean
    public SecretKey aesSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("TOKEN_ENCRYPTION_KEY debe ser una clave Base64 de 32 bytes (256 bits)");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
