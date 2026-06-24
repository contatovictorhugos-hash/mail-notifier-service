package br.com.mailnotifier.util;

import br.com.mailnotifier.exception.EmailEncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

    private CryptoUtils cryptoUtils;

    @BeforeEach
    void setUp() {
        cryptoUtils = new CryptoUtils();
    }

    @Test
    @DisplayName("Deve criptografar conteúdo com chave pública RSA válida")
    void encryptHybrid_comChaveValida_retornaResultado() throws Exception {
        // Arrange - Gerar um par de chaves RSA real
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String textoOriginal = "Este é um conteúdo secreto para teste";

        // Act
        CryptoUtils.EncryptionResult result = cryptoUtils.encryptHybrid(textoOriginal, publicKeyBase64);

        // Assert
        assertNotNull(result);
        assertNotNull(result.encryptedContent());
        assertNotNull(result.encryptedAesKey());
        assertFalse(result.encryptedContent().isEmpty());
        assertFalse(result.encryptedAesKey().isEmpty());

        // Verifica que o conteúdo criptografado é diferente do original
        assertNotEquals(textoOriginal, result.encryptedContent());

        // Verifica que são strings Base64 válidas
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result.encryptedContent()));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result.encryptedAesKey()));
    }

    @Test
    @DisplayName("Deve aceitar chave pública com marcadores PEM")
    void encryptHybrid_comChavePEM_retornaResultado() throws Exception {
        // Arrange
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + publicKeyBase64 + "\n-----END PUBLIC KEY-----";

        // Act
        CryptoUtils.EncryptionResult result = cryptoUtils.encryptHybrid("Texto PEM", publicKeyPem);

        // Assert
        assertNotNull(result);
        assertFalse(result.encryptedContent().isEmpty());
        assertFalse(result.encryptedAesKey().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar EmailEncryptionException com chave inválida (Base64 inválido)")
    void encryptHybrid_comChaveInvalida_lancaExcecao() {
        // Act & Assert
        assertThrows(EmailEncryptionException.class,
                () -> cryptoUtils.encryptHybrid("texto", "chave-invalida-!!!"));
    }

    @Test
    @DisplayName("Deve lançar EmailEncryptionException com chave Base64 válido mas estrutura RSA inválida")
    void encryptHybrid_comChaveRSAInvalida_lancaExcecao() {
        // Base64 válido mas não é uma chave RSA
        String fakeKey = Base64.getEncoder().encodeToString("isto não é uma chave RSA".getBytes());

        // Act & Assert
        assertThrows(EmailEncryptionException.class,
                () -> cryptoUtils.encryptHybrid("texto", fakeKey));
    }
}
