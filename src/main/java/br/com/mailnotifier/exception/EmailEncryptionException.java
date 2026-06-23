package br.com.mailnotifier.exception;

public class EmailEncryptionException extends RuntimeException {
    public EmailEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
