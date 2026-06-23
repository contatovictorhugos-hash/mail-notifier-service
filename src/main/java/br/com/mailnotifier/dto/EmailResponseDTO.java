package br.com.mailnotifier.dto;

import br.com.mailnotifier.model.EmailStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmailResponseDTO(
                UUID id,
                String recipient,
                String subject,
                EmailStatus status,
                LocalDateTime sentAt,
                Boolean encrypted) {
}
