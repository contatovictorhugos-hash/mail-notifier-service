package br.com.mailnotifier.exception;

import br.com.mailnotifier.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiError> handleEmailDeliveryException(EmailDeliveryException ex) {
        log.error("Falha na integração com provedor de e-mail: {}", ex.getMessage(), ex);

        ApiError error = new ApiError(
                HttpStatus.BAD_GATEWAY.value(),
                "Bad Gateway",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

}
