package br.com.mailnotifier.model;

import jakarta.persistence.*;
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

    private String recipient;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private EmailStatus status;

    private LocalDateTime sentAt;

    private Boolean encrypted = false;

    public Email(String recipient, String subject, String content, Boolean encrypted) {
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.status = EmailStatus.PENDING;
        this.sentAt = LocalDateTime.now();
        this.encrypted = encrypted;
    }
}
