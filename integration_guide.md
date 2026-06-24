# Guia de Integração: Novo Fluxo de Criptografia Híbrida

Este documento descreve as alterações necessárias no projeto **`mailNotifierService`** para integrar-se com o **`key-management-service`** quando o envio criptografado for solicitado.

---

## Novo Fluxo Logístico

1. **Requisição de Envio**: O cliente envia um JSON para o `mailNotifierService` com o campo `encrypted` setado como `true` ou `false`.
2. **Caso `encrypted = false` (Padrão)**:
   - O e-mail é processado, salvo no banco e enviado em texto claro (fluxo padrão existente).
   - O campo `keyId` no JSON de resposta retorna `null`.
3. **Caso `encrypted = true`**:
   - O `mailNotifierService` faz uma chamada HTTP POST para o `key-management-service` (`POST http://localhost:8081/keys`) para gerar um novo par de chaves.
   - O serviço obtém da resposta o `keyId` (UUID) e a `publicKey` (Base64).
   - Realiza a encriptação híbrida do conteúdo usando a `publicKey`.
   - Salva o e-mail criptografado no banco de dados.
   - Envia o e-mail via cliente de SMTP (Brevo) contendo o payload híbrido: `EncriptKey.EncriptText`.
   - O JSON de resposta do endpoint de envio de e-mails retorna o `keyId` associado.

---

## Alterações Propostas no `mailNotifierService`

### 1. Atualização do DTO de Entrada (`EmailRequestDTO`)

Substituir o campo `publicKey` (String) por `encrypted` (Boolean). Este campo pode ser opcional ou omitido no JSON (nulo). Caso não seja informado (ou seja `false`), o sistema seguirá o fluxo padrão sem criptografia.

```java
package br.com.mailnotifier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequestDTO(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient must be a valid email address")
        String recipient,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Content is required")
        String content,

        Boolean encrypted
) {}
```

---

### 2. Atualização da Entidade `Email` e Tabela no Banco de Dados

Precisamos armazenar o `keyId` no banco de dados para podermos saber qual chave descriptografa cada e-mail.

#### Alteração na Entidade `Email.java`:
```java
// Adicionar o atributo na classe Email:
private UUID keyId;
```

#### Script de Migração SQL (Flyway):
Crie uma nova migration (ex: `V2__add_key_id_to_emails.sql`) para adicionar a coluna:
```sql
ALTER TABLE emails ADD COLUMN key_id UUID;
```

---

### 3. Atualização do DTO de Retorno (`EmailResponseDTO`)

O cliente precisa receber o `keyId` para saber como descriptografar o e-mail futuramente.

```java
package br.com.mailnotifier.dto;

import br.com.mailnotifier.model.EmailStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmailResponseDTO(
        Long id,
        String recipient,
        String subject,
        EmailStatus status,
        LocalDateTime sentAt,
        Boolean encrypted,
        UUID keyId
) {}
```

---

### 4. Implementação do Client HTTP para o `key-management-service`

Crie um client simples utilizando `RestTemplate` ou `WebClient` para chamar o serviço de chaves.

#### Criar `KeyResponseDTO.java` no pacote de DTOs:
```java
package br.com.mailnotifier.dto;

import java.util.UUID;

public record KeyResponseDTO(
        UUID keyId,
        String publicKey
) {}
```

#### Criar `KeyManagementClient.java` (ou integrar no serviço):
```java
package br.com.mailnotifier.client;

import br.com.mailnotifier.dto.KeyResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeyManagementClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String KEY_SERVICE_URL = "http://localhost:8081/keys";

    public KeyResponseDTO generateKeys() {
        return restTemplate.postForObject(KEY_SERVICE_URL, null, KeyResponseDTO.class);
    }
}
```

---

### 5. Adaptação do `EmailService.java`

Modifique a lógica do método `processarNotificacao` para coordenar o fluxo:

```java
package br.com.mailnotifier.service;

import br.com.mailnotifier.client.BrevoEmailClient;
import br.com.mailnotifier.client.KeyManagementClient;
import br.com.mailnotifier.dto.EmailRequestDTO;
import br.com.mailnotifier.dto.KeyResponseDTO;
import br.com.mailnotifier.model.Email;
import br.com.mailnotifier.model.EmailStatus;
import br.com.mailnotifier.repository.EmailRepository;
import br.com.mailnotifier.util.CryptoUtils;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final BrevoEmailClient emailClient;
    private final CryptoUtils cryptoUtils;
    private final KeyManagementClient keyManagementClient; // Injetar o novo client

    public EmailService(EmailRepository emailRepository, BrevoEmailClient emailClient, 
                        CryptoUtils cryptoUtils, KeyManagementClient keyManagementClient) {
        this.emailRepository = emailRepository;
        this.emailClient = emailClient;
        this.cryptoUtils = cryptoUtils;
        this.keyManagementClient = keyManagementClient;
    }

    public Email processarNotificacao(EmailRequestDTO dto) {
        String conteudoParaBanco = dto.content();
        String conteudoParaEnvio = dto.content();
        boolean encrypted = false;
        UUID keyId = null;

        // Se a criptografia for solicitada
        if (Boolean.TRUE.equals(dto.encrypted())) {
            // 1. Chamar o microserviço para pegar a nova chave e ID
            KeyResponseDTO keyInfo = keyManagementClient.generateKeys();
            keyId = keyInfo.keyId();

            // 2. Realizar a criptografia híbrida com a chave pública retornada
            CryptoUtils.EncryptionResult result = cryptoUtils.encryptHybrid(dto.content(), keyInfo.publicKey());
            conteudoParaBanco = result.encryptedContent();
            
            // 3. Montar o conteúdo para envio formatado como EncriptKey.EncriptText
            conteudoParaEnvio = result.encryptedAesKey() + "." + result.encryptedContent();
            encrypted = true;
        }

        // 4. Instanciar e salvar o e-mail no banco local com a flag e o keyId associado
        Email email = new Email(dto.recipient(), dto.subject(), conteudoParaBanco, encrypted);
        email.setKeyId(keyId); // Setar o keyId gerado
        email = saveEmail(email);

        // 5. Enviar o e-mail final
        try {
            emailClient.enviar(email, conteudoParaEnvio);
            email.setStatus(EmailStatus.SENT);
            return emailRepository.save(email);
        } catch (Exception e) {
            email.setStatus(EmailStatus.ERROR);
            emailRepository.save(email);
            throw new RuntimeException("Falha ao enviar e-mail para " + email.getRecipient(), e);
        }
    }

    private Email saveEmail(Email email) {
        return emailRepository.save(email);
    }
}
```

---

### 6. Atualização do `EmailController.java`

Mapeie o `keyId` da entidade na resposta:

```java
    @PostMapping
    public ResponseEntity<EmailResponseDTO> enviarEmail(@Valid @RequestBody EmailRequestDTO dto) {

        Email email = emailService.processarNotificacao(dto);

        EmailResponseDTO response = new EmailResponseDTO(
                email.getId(),
                email.getRecipient(),
                email.getSubject(),
                email.getStatus(),
                email.getSentAt(),
                email.getEncrypted(),
                email.getKeyId() // Retorna o UUID da chave associada
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
```
