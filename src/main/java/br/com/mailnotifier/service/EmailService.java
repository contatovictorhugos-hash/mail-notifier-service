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
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final BrevoEmailClient emailClient;
    private final CryptoUtils cryptoUtils;
    private final KeyManagementClient keyManagementClient;

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

        if (Boolean.TRUE.equals(dto.encrypted())) {
            KeyResponseDTO keyInfo = keyManagementClient.generateKeys();
            keyId = keyInfo.keyId();

            CryptoUtils.EncryptionResult result = cryptoUtils.encryptHybrid(dto.content(), keyInfo.publicKey());
            conteudoParaBanco = result.encryptedContent();

            conteudoParaEnvio = keyId + "." + result.encryptedAesKey() + "." + result.encryptedContent();
            encrypted = true;
        }

        Email email = new Email(dto.recipient(), dto.subject(), conteudoParaBanco, encrypted);
        email.setKeyId(keyId);
        email = saveEmail(email);
        try {
            emailClient.enviar(email, conteudoParaEnvio);
            email.setStatus(EmailStatus.SENT);
            return emailRepository.save(email);
        } catch (Exception e) {
            email.setStatus(EmailStatus.ERROR);
            emailRepository.save(email);
            throw new EmailDeliveryException("Falha ao enviar e-mail para " + email.getRecipient(), e);
        }
    }

    private Email saveEmail(Email email) {
        return emailRepository.save(email);
    }

}
