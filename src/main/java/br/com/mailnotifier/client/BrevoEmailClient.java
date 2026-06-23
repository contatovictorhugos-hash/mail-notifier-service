package br.com.mailnotifier.client;

import br.com.mailnotifier.config.BrevoProperties;
import br.com.mailnotifier.model.Email;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BrevoEmailClient {

    private final RestClient restClient;
    private final BrevoProperties properties;

    public BrevoEmailClient(BrevoProperties properties,
                            @Nullable HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        this.properties = properties;

        HttpComponentsClientHttpRequestFactory factory =
                httpRequestFactory != null ? httpRequestFactory : new HttpComponentsClientHttpRequestFactory();

        this.restClient = RestClient.builder()
                .baseUrl(properties.apiUrl())
                .requestFactory(factory)
                .build();
    }

    public void enviar(Email email) {
        BrevoRequest payload = new BrevoRequest(
                new BrevoRequest.Sender(properties.senderName(), properties.senderEmail()),
                List.of(new BrevoRequest.To(email.getDestinatario())),
                email.getTitulo(),
                email.getConteudo());

        restClient.post()
                .header("api-key", properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private record BrevoRequest(
            Sender sender,
            List<To> to,
            String subject,
            String htmlContent) {
        public record Sender(String name, String email) {
        }

        public record To(String email) {
        }
    }
}
