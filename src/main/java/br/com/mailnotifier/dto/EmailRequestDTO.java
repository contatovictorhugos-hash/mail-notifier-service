package br.com.mailnotifier.dto;

public record EmailRequestDTO(
        String destinatario,
        String titulo,
        String conteudo) {
}
