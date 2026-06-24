package br.com.mailnotifier.service;

import br.com.mailnotifier.client.BrevoEmailClient;
import br.com.mailnotifier.client.KeyManagementClient;
import br.com.mailnotifier.dto.EmailRequestDTO;
import br.com.mailnotifier.dto.KeyResponseDTO;
import br.com.mailnotifier.exception.EmailDeliveryException;
import br.com.mailnotifier.model.Email;
import br.com.mailnotifier.model.EmailStatus;
import br.com.mailnotifier.repository.EmailRepository;
import br.com.mailnotifier.util.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private BrevoEmailClient emailClient;

    @Mock
    private CryptoUtils cryptoUtils;

    @Mock
    private KeyManagementClient keyManagementClient;

    @InjectMocks
    private EmailService emailService;

    private EmailRequestDTO dtoSemCriptografia;
    private EmailRequestDTO dtoComCriptografia;

    @BeforeEach
    void setUp() {
        dtoSemCriptografia = new EmailRequestDTO(
                "dest@email.com", "Assunto Teste", "Conteúdo em texto claro", false);

        dtoComCriptografia = new EmailRequestDTO(
                "dest@email.com", "Assunto Seguro", "Conteúdo secreto", true);
    }

    private void stubSaveEmail() {
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> {
            Email e = invocation.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
    }

    @Test
    @DisplayName("Deve enviar e-mail em texto claro quando encrypted=false")
    void processarNotificacao_semCriptografia_enviaTextoClaro() {
        // Arrange
        stubSaveEmail();

        // Act
        Email resultado = emailService.processarNotificacao(dtoSemCriptografia);

        // Assert
        assertEquals(EmailStatus.SENT, resultado.getStatus());
        assertFalse(resultado.getEncrypted());
        assertNull(resultado.getKeyId());
        assertEquals("Conteúdo em texto claro", resultado.getContent());

        // Verifica que o KeyManagementClient NÃO foi chamado
        verify(keyManagementClient, never()).generateKeys();

        // Verifica que o CryptoUtils NÃO foi chamado
        verify(cryptoUtils, never()).encryptHybrid(anyString(), anyString());

        // Verifica que o e-mail foi enviado com o conteúdo original
        verify(emailClient).enviar(any(Email.class), eq("Conteúdo em texto claro"));

        // Verifica que save foi chamado 2x (criação + atualização de status)
        verify(emailRepository, times(2)).save(any(Email.class));
    }

    @Test
    @DisplayName("Deve enviar e-mail em texto claro quando encrypted=null")
    void processarNotificacao_encryptedNull_enviaTextoClaro() {
        // Arrange
        stubSaveEmail();

        EmailRequestDTO dtoNull = new EmailRequestDTO(
                "dest@email.com", "Assunto", "Conteúdo", null);

        // Act
        Email resultado = emailService.processarNotificacao(dtoNull);

        // Assert
        assertFalse(resultado.getEncrypted());
        assertNull(resultado.getKeyId());
        verify(keyManagementClient, never()).generateKeys();
    }

    @Test
    @DisplayName("Deve chamar KeyManagementClient e criptografar quando encrypted=true")
    void processarNotificacao_comCriptografia_chamaKeyServiceECriptografa() {
        // Arrange
        stubSaveEmail();

        UUID expectedKeyId = UUID.randomUUID();
        String publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...";
        KeyResponseDTO keyResponse = new KeyResponseDTO(expectedKeyId, publicKeyBase64);

        when(keyManagementClient.generateKeys()).thenReturn(keyResponse);
        when(cryptoUtils.encryptHybrid("Conteúdo secreto", publicKeyBase64))
                .thenReturn(new CryptoUtils.EncryptionResult("encryptedContent123", "encryptedKey456"));

        // Act
        Email resultado = emailService.processarNotificacao(dtoComCriptografia);

        // Assert
        assertEquals(EmailStatus.SENT, resultado.getStatus());
        assertTrue(resultado.getEncrypted());
        assertEquals(expectedKeyId, resultado.getKeyId());
        assertEquals("encryptedContent123", resultado.getContent());

        // Verifica que o KeyManagementClient FOI chamado
        verify(keyManagementClient).generateKeys();

        // Verifica a criptografia
        verify(cryptoUtils).encryptHybrid("Conteúdo secreto", publicKeyBase64);

        // Verifica que o e-mail foi enviado com o payload híbrido: KeyId.EncriptKey.EncriptText
        verify(emailClient).enviar(any(Email.class), eq(expectedKeyId + ".encryptedKey456.encryptedContent123"));
    }

    @Test
    @DisplayName("Deve setar status ERROR e lançar exceção quando o envio falha")
    void processarNotificacao_falhaEnvio_setaStatusError() {
        // Arrange
        stubSaveEmail();

        doThrow(new RuntimeException("SMTP timeout"))
                .when(emailClient).enviar(any(Email.class), anyString());

        // Act & Assert
        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class,
                () -> emailService.processarNotificacao(dtoSemCriptografia));

        assertTrue(ex.getMessage().contains("dest@email.com"));

        // Verifica que o e-mail foi salvo com status ERROR
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository, times(2)).save(captor.capture());

        Email emailSalvo = captor.getAllValues().get(1); // segunda chamada ao save
        assertEquals(EmailStatus.ERROR, emailSalvo.getStatus());
    }

    @Test
    @DisplayName("Deve propagar exceção quando o key-management-service falha")
    void processarNotificacao_falhaKeyService_lancaExcecao() {
        // Arrange
        when(keyManagementClient.generateKeys())
                .thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> emailService.processarNotificacao(dtoComCriptografia));

        // Verifica que nenhum e-mail foi salvo (a exceção ocorre antes do save)
        verify(emailRepository, never()).save(any(Email.class));
        verify(emailClient, never()).enviar(any(Email.class), anyString());
    }
}

