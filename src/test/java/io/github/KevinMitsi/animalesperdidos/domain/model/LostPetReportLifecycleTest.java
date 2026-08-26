package io.github.KevinMitsi.animalesperdidos.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LostPetReportLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void removingPrimaryPromotesFirstRemainingImage() {
        LostPetReport report = report(List.of("one", "two"));

        LostPetReport updated = report.removeImage(report.images().getFirst().id(), NOW.plusSeconds(1));

        assertEquals(1, updated.images().size());
        assertTrue(updated.images().getFirst().primary());
        assertEquals(0, updated.images().getFirst().sortOrder());
    }

    @Test
    void aClosedReportCannotBeEdited() {
        LostPetReport closed = report(List.of("one")).changeStatus(ReportStatus.REUNITED, NOW.plusSeconds(1));

        assertThrows(IllegalStateException.class, () -> closed.edit("Luna", Species.DOG, "x",
                NOW.minusSeconds(20), new GeoPoint(4.5, -75.6),
                new AdministrativeLocation("63","63001","Granada"), NOW.plusSeconds(2)));
    }

    @Test
    void aRecentlyClosedReportCanBeReopenedButNotAfterThirtyDays() {
        LostPetReport closed = report(List.of("one")).changeStatus(ReportStatus.CLOSED, NOW.plusSeconds(1));

        LostPetReport reopened = closed.reopen(NOW.plus(Duration.ofDays(20)), Duration.ofDays(30));

        assertEquals(ReportStatus.LOST, reopened.status());
        assertThrows(IllegalStateException.class,
                () -> closed.reopen(NOW.plus(Duration.ofDays(31)), Duration.ofDays(30)));
    }

    @Test
    void moderatorConfirmedReunionCannotBeReopenedByOwner() {
        LostPetReport reunited = report(List.of("one")).changeStatus(ReportStatus.REUNITED, NOW.plusSeconds(1));
        assertThrows(IllegalStateException.class,
                () -> reunited.reopen(NOW.plus(Duration.ofDays(1)), Duration.ofDays(30)));
    }

    private static LostPetReport report(List<String> keys) {
        return LostPetReport.create(UUID.randomUUID(), UUID.randomUUID(), "Luna", Species.DOG, "Collar rojo",
                NOW.minusSeconds(3600), new GeoPoint(4.5339, -75.6811),
                new AdministrativeLocation("63","63001","Granada"), keys, NOW);
    }
}
