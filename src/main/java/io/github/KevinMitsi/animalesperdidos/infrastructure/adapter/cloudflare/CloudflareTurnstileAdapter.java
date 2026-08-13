package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.cloudflare;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.KevinMitsi.animalesperdidos.application.port.out.BotVerificationPort;
import io.github.KevinMitsi.animalesperdidos.infrastructure.config.CloudflareProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cloudflare.turnstile.enabled", havingValue = "true")
public class CloudflareTurnstileAdapter implements BotVerificationPort {
    private final WebClient cloudflareWebClient;
    private final CloudflareProperties properties;

    @Override
    public CompletionStage<Boolean> verify(String token, String remoteIp, String expectedAction) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.getSecretKey());
        form.add("response", token);
        form.add("idempotency_key", UUID.randomUUID().toString());
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.add("remoteip", remoteIp);
        }
        return cloudflareWebClient.post()
                .uri("/turnstile/v0/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(TurnstileResponse.class)
                .map(response -> response.success()
                        && expectedAction.equals(response.action())
                        && hostnameMatches(response.hostname()))
                .onErrorReturn(false)
                .defaultIfEmpty(false)
                .toFuture();
    }

    private boolean hostnameMatches(String hostname) {
        String expected = properties.getExpectedHostname();
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(hostname);
    }

    record TurnstileResponse(boolean success, String hostname, String action,
                             @JsonProperty("error-codes") List<String> errorCodes) {
    }
}
