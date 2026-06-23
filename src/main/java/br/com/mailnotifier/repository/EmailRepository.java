package br.com.mailnotifier.repository;

import br.com.mailnotifier.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<Email, UUID> {
}
