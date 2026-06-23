package br.com.mailnotifier.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDateTime;
import java.util.UUID;
import br.com.mailnotifier.dto.EmailRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String destinatario;
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    private StatusEmail statusEmail;

    private LocalDateTime dataEnvio;

    public Email(EmailRequestDTO dto) {
        this.destinatario = dto.destinatario();
        this.titulo = dto.titulo();
        this.conteudo = dto.conteudo();
        this.statusEmail = StatusEmail.PENDENTE;
        this.dataEnvio = LocalDateTime.now();
    }

}
