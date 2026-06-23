package br.com.mailnotifier.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;

@Configuration
@Profile("dev")
public class SslConfig {

    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory() throws Exception {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(
                                        SSLConnectionSocketFactoryBuilder.create()
                                                .setSslContext(sslContext)
                                                .build()
                                )
                                .build()
                )
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

}
