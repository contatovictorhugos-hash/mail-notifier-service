package br.com.mailnotifier.controller;

import br.com.mailnotifier.dto.EmailRequestDTO;
import br.com.mailnotifier.dto.EmailResponseDTO;
import br.com.mailnotifier.model.Email;
import br.com.mailnotifier.service.EmailService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<EmailResponseDTO> enviarEmail(@RequestBody EmailRequestDTO dto) {

        Email email = emailService.processarNotificacao(dto);

        EmailResponseDTO response = new EmailResponseDTO(email.getId(), email.getDestinatario(), email.getTitulo(),
                email.getStatusEmail(), email.getDataEnvio());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
