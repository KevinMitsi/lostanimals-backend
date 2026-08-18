package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebConfigurationTest {

    @Test
    void corsAllowsOnlyConfiguredFrontendOrigins() {
        WebCorsProperties properties = new WebCorsProperties();
        properties.setAllowedOrigins(List.of("https://www.animales-perdidos.com"));
        var source = new WebConfiguration().corsConfigurationSource(properties);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/geography/service-areas").build());

        var configuration = source.getCorsConfiguration(exchange);

        assertNotNull(configuration);
        assertEquals(List.of("https://www.animales-perdidos.com"), configuration.getAllowedOrigins());
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
    }
}
