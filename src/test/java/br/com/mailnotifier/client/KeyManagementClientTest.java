package br.com.mailnotifier.client;

import br.com.mailnotifier.dto.KeyResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyManagementClientTest {

    @Mock
    private RestTemplate restTemplate;

    private KeyManagementClient keyManagementClient;

    private static final String KEY_SERVICE_URL = "http://localhost:8081/keys";

    @BeforeEach
    void setUp() {
        // Usamos uma subclasse anônima para injetar o RestTemplate mockado
        keyManagementClient = new KeyManagementClient(KEY_SERVICE_URL) {
            // sobrescreveremos via reflection no teste, mas como o RestTemplate é
            // criado no construtor, usaremos o Mockito spy approach
        };
        // Injetar o mock de RestTemplate via reflection
        try {
            var field = KeyManagementClient.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(keyManagementClient, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao injetar mock RestTemplate", e);
        }
    }

    @Test
    @DisplayName("Deve chamar o key-management-service e retornar KeyResponseDTO")
    void generateKeys_comRespostaValida_retornaKeyResponse() {
        // Arrange
        UUID expectedKeyId = UUID.randomUUID();
        String expectedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...";
        KeyResponseDTO expectedResponse = new KeyResponseDTO(expectedKeyId, expectedPublicKey);

        when(restTemplate.postForObject(eq(KEY_SERVICE_URL), isNull(), eq(KeyResponseDTO.class)))
                .thenReturn(expectedResponse);

        // Act
        KeyResponseDTO result = keyManagementClient.generateKeys();

        // Assert
        assertNotNull(result);
        assertEquals(expectedKeyId, result.keyId());
        assertEquals(expectedPublicKey, result.publicKey());
        verify(restTemplate).postForObject(eq(KEY_SERVICE_URL), isNull(), eq(KeyResponseDTO.class));
    }

    @Test
    @DisplayName("Deve propagar exceção quando o serviço de chaves está indisponível")
    void generateKeys_comServicoIndisponivel_lancaExcecao() {
        // Arrange
        when(restTemplate.postForObject(eq(KEY_SERVICE_URL), isNull(), eq(KeyResponseDTO.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        assertThrows(RestClientException.class, () -> keyManagementClient.generateKeys());
    }
}
