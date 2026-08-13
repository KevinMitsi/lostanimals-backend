package io.github.kevinmitsi.animalesperdidos.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LostPetReportTest {

    @Test
    void rejectsAFutureDisappearance() {
        Instant now = Instant.parse("2026-08-13T12:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> LostPetReport.create(
                UUID.randomUUID(), UUID.randomUUID(), "Luna", Species.DOG, "Collar rojo",
                now.plusSeconds(1), new GeoPoint(4.5339, -75.6811), UUID.randomUUID(),
                List.of("reports/luna.jpg"), now));
    }

    @Test
    void requiresAtLeastOneImage() {
        Instant now = Instant.parse("2026-08-13T12:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> LostPetReport.create(
                UUID.randomUUID(), UUID.randomUUID(), "Luna", Species.DOG, "Collar rojo",
                now, new GeoPoint(4.5339, -75.6811), UUID.randomUUID(), List.of(), now));
    }
}
