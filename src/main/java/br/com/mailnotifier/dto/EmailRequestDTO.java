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

                Boolean encrypted) {
}
