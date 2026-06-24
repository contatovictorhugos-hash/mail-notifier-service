package br.com.mailnotifier.client;

import br.com.mailnotifier.dto.KeyResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeyManagementClient {

    private final RestTemplate restTemplate;
    private final String keyServiceUrl;

    public KeyManagementClient(
            @Value("${key-management.url:http://localhost:8081/keys}") String keyServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.keyServiceUrl = keyServiceUrl;
    }

    public KeyResponseDTO generateKeys() {
        return restTemplate.postForObject(keyServiceUrl, null, KeyResponseDTO.class);
    }
}
