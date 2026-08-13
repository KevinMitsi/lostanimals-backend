package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(CloudflareProperties.class)
public class CloudflareConfiguration {

    @Bean
    WebClient cloudflareWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://challenges.cloudflare.com").build();
    }
}
