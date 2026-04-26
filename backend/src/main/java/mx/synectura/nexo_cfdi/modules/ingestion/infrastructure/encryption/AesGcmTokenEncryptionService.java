package mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.encryption;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AesGcmTokenEncryptionService {

    private static final int GCM_IV_LENGTH  = 12;  // 96 bits (recomendado para GCM)
    private static final int GCM_TAG_LENGTH = 128; // bits

    private final SecretKey aesSecretKey;

    public EncryptedToken encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesSecretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedToken(
                    Base64.getEncoder().encodeToString(cipherBytes),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar token", e);
        }
    }

    public String decrypt(String ciphertextBase64, String ivBase64) {
        try {
            byte[] iv         = Base64.getDecoder().decode(ivBase64);
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertextBase64);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesSecretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error al descifrar token", e);
        }
    }

    public record EncryptedToken(String ciphertext, String iv) {}
}
