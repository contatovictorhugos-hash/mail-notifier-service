package br.com.mailnotifier.dto;

public record EmailRequestDTO(
                String recipient,
                String subject,
                String content,
                String publicKey) {
}
