package br.com.mailnotifier.util;

import br.com.mailnotifier.exception.EmailEncryptionException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class CryptoUtils {

    public EncryptionResult encryptHybrid(String plainText, String publicKeyPem) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
            byte[] encryptedContentBytes = aesCipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] combinedAesOutput = new byte[iv.length + encryptedContentBytes.length];
            System.arraycopy(iv, 0, combinedAesOutput, 0, iv.length);
            System.arraycopy(encryptedContentBytes, 0, combinedAesOutput, iv.length, encryptedContentBytes.length);
            String encryptedContentBase64 = Base64.getEncoder().encodeToString(combinedAesOutput);

            String cleanKeyPem = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] rsaKeyBytes = Base64.getDecoder().decode(cleanKeyPem);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(rsaKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey rsaPublicKey = kf.generatePublic(spec);

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            byte[] encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.getEncoded());
            String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);

            return new EncryptionResult(encryptedContentBase64, encryptedAesKeyBase64);

        } catch (Exception e) {
            throw new EmailEncryptionException("Erro ao processar criptografia híbrida", e);
        }
    }

    public record EncryptionResult(String encryptedContent, String encryptedAesKey) {
    }
}
