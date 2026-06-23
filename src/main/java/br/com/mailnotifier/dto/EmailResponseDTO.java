package br.com.mailnotifier.dto;

import br.com.mailnotifier.model.StatusEmail;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmailResponseDTO(
        UUID id,
        String destinatario,
        String titulo,
        StatusEmail statusEmail,
        LocalDateTime dataEnvio) {
}
