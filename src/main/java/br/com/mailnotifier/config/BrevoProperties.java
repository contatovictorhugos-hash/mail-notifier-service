package br.com.mailnotifier.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "brevo")
@Validated
public record BrevoProperties(
        @NotBlank String apiUrl,
        @NotBlank String apiKey,
        @NotBlank String senderEmail,
        @NotBlank String senderName
) {}
