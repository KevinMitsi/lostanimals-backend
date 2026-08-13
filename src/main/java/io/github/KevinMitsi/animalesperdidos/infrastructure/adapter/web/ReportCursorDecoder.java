package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import org.springframework.web.server.ServerWebInputException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

final class ReportCursorDecoder {
    private ReportCursorDecoder() { }

    static Cursor decode(String value) {
        if (value == null || value.isBlank()) return new Cursor(null, null);
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int separator = decoded.lastIndexOf('|');
            if (separator < 1) throw new IllegalArgumentException();
            return new Cursor(Instant.parse(decoded.substring(0, separator)),
                    UUID.fromString(decoded.substring(separator + 1)));
        } catch (RuntimeException error) {
            throw new ServerWebInputException("Invalid pagination cursor");
        }
    }

    record Cursor(Instant createdAt, UUID id) { }
}
