package br.com.mailnotifier.dto;

import java.util.UUID;

public record KeyResponseDTO(
        UUID keyId,
        String publicKey) {
}
