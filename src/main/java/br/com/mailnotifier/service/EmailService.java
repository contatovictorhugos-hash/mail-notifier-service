package br.com.mailnotifier.service;

import br.com.mailnotifier.client.BrevoEmailClient;
import br.com.mailnotifier.dto.EmailRequestDTO;
import br.com.mailnotifier.exception.EmailDeliveryException;
import br.com.mailnotifier.model.Email;
import br.com.mailnotifier.model.EmailStatus;
import br.com.mailnotifier.repository.EmailRepository;
import br.com.mailnotifier.util.CryptoUtils;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final BrevoEmailClient emailClient;
    private final CryptoUtils cryptoUtils;

    public EmailService(EmailRepository emailRepository, BrevoEmailClient emailClient, CryptoUtils cryptoUtils) {
        this.emailRepository = emailRepository;
        this.emailClient = emailClient;
        this.cryptoUtils = cryptoUtils;
    }

    public Email processarNotificacao(EmailRequestDTO dto) {
        String conteudoParaBanco = dto.content();
        String conteudoParaEnvio = dto.content();
        boolean encrypted = false;

        if (dto.publicKey() != null && !dto.publicKey().isBlank()) {
            CryptoUtils.EncryptionResult result = cryptoUtils.encryptHybrid(dto.content(), dto.publicKey());
            conteudoParaBanco = result.encryptedContent();
            conteudoParaEnvio = result.encryptedAesKey() + "." + result.encryptedContent();
            encrypted = true;
        }

        Email email = new Email(dto.recipient(), dto.subject(), conteudoParaBanco, encrypted);
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
