package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.Event;
import io.github.KevinMitsi.animalesperdidos.application.port.out.NotificationEventPublisher.Type;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonConfigurationTest {

    @Test
    void objectMapperSupportsNotificationEventsWithJavaTime() throws Exception {
        Event expected = new Event(UUID.randomUUID(), Type.EMAIL_VERIFICATION, null, "ana@example.com",
                Map.of("token", "one-use-token"), Instant.parse("2026-08-18T12:00:00Z"));
        var mapper = new JsonConfiguration().objectMapper();

        Event actual = mapper.readValue(mapper.writeValueAsString(expected), Event.class);

        assertEquals(expected, actual);
    }
}
