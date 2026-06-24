package br.com.mailnotifier.controller;

import br.com.mailnotifier.model.Email;
import br.com.mailnotifier.model.EmailStatus;
import br.com.mailnotifier.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deve retornar 201 com keyId=null quando encrypted=false")
    void enviarEmail_semCriptografia_retorna201SemKeyId() throws Exception {
        // Arrange
        Email emailMock = new Email("dest@email.com", "Assunto", "Conteúdo", false);
        emailMock.setId(UUID.randomUUID());
        emailMock.setStatus(EmailStatus.SENT);
        emailMock.setSentAt(LocalDateTime.now());

        when(emailService.processarNotificacao(any())).thenReturn(emailMock);

        String requestBody = """
                {
                    "recipient": "dest@email.com",
                    "subject": "Assunto",
                    "content": "Conteúdo",
                    "encrypted": false
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.recipient").value("dest@email.com"))
                .andExpect(jsonPath("$.subject").value("Assunto"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.encrypted").value(false))
                .andExpect(jsonPath("$.keyId").doesNotExist());
    }

    @Test
    @DisplayName("Deve retornar 201 com keyId preenchido quando encrypted=true")
    void enviarEmail_comCriptografia_retorna201ComKeyId() throws Exception {
        // Arrange
        UUID expectedKeyId = UUID.randomUUID();
        Email emailMock = new Email("dest@email.com", "Assunto Seguro", "encryptedContent", true);
        emailMock.setId(UUID.randomUUID());
        emailMock.setStatus(EmailStatus.SENT);
        emailMock.setSentAt(LocalDateTime.now());
        emailMock.setKeyId(expectedKeyId);

        when(emailService.processarNotificacao(any())).thenReturn(emailMock);

        String requestBody = """
                {
                    "recipient": "dest@email.com",
                    "subject": "Assunto Seguro",
                    "content": "Conteúdo secreto",
                    "encrypted": true
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.encrypted").value(true))
                .andExpect(jsonPath("$.keyId").value(expectedKeyId.toString()));
    }

    @Test
    @DisplayName("Deve retornar 400 quando campos obrigatórios estão faltando")
    void enviarEmail_comDadosInvalidos_retorna400() throws Exception {
        String requestBody = """
                {
                    "recipient": "",
                    "subject": "",
                    "content": ""
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 quando e-mail do destinatário é inválido")
    void enviarEmail_comEmailInvalido_retorna400() throws Exception {
        String requestBody = """
                {
                    "recipient": "email-invalido",
                    "subject": "Assunto",
                    "content": "Conteúdo"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve funcionar quando encrypted é omitido no JSON (null)")
    void enviarEmail_semCampoEncrypted_retorna201() throws Exception {
        // Arrange
        Email emailMock = new Email("dest@email.com", "Assunto", "Conteúdo", false);
        emailMock.setId(UUID.randomUUID());
        emailMock.setStatus(EmailStatus.SENT);
        emailMock.setSentAt(LocalDateTime.now());

        when(emailService.processarNotificacao(any())).thenReturn(emailMock);

        String requestBody = """
                {
                    "recipient": "dest@email.com",
                    "subject": "Assunto",
                    "content": "Conteúdo"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encrypted").value(false));
    }
}
