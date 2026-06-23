package br.com.mailnotifier.service;

import br.com.mailnotifier.client.BrevoEmailClient;
import br.com.mailnotifier.dto.EmailRequestDTO;
import br.com.mailnotifier.exception.EmailDeliveryException;
import br.com.mailnotifier.model.Email;
import br.com.mailnotifier.model.StatusEmail;
import br.com.mailnotifier.repository.EmailRepository;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final BrevoEmailClient emailClient;

    public EmailService(EmailRepository emailRepository, BrevoEmailClient emailClient) {
        this.emailRepository = emailRepository;
        this.emailClient = emailClient;
    }

    public Email processarNotificacao(EmailRequestDTO dto) {
        Email email = new Email(dto);
        email = saveEmail(email);

        try {
            emailClient.enviar(email);
            email.setStatusEmail(StatusEmail.ENVIADO);
            return emailRepository.save(email);
        } catch (Exception e) {
            email.setStatusEmail(StatusEmail.ERRO);
            emailRepository.save(email);
            throw new EmailDeliveryException("Falha ao enviar e-mail para " + email.getDestinatario(), e);
        }
    }

    private Email saveEmail(Email email) {
        return emailRepository.save(email);
    }

}

