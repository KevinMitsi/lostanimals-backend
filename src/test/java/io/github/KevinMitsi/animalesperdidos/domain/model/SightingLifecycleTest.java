package io.github.KevinMitsi.animalesperdidos.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SightingLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test void createsActiveAndPromotesAnotherPrimaryWhenRemovingIt() {
        Sighting original = sighting();
        UUID second = UUID.randomUUID();
        Sighting withSecond = original.addImage(new SightingImage(second, "sightings/users/u/2.jpg", false, 1), NOW.plusSeconds(1));
        Sighting result = withSecond.removeImage(withSecond.images().getFirst().id(), NOW.plusSeconds(2));
        assertEquals(SightingStatus.ACTIVE, result.status());
        assertEquals(second, result.images().getFirst().id());
        assertTrue(result.images().getFirst().primary());
    }

    @Test void closedSightingsCannotBeEdited() {
        Sighting closed = sighting().close(NOW.plusSeconds(1));
        assertThrows(IllegalStateException.class, () -> closed.edit(Species.CAT, "Nueva descripción",
                NOW.minusSeconds(60), closed.location(), closed.administrativeLocation(), NOW.plusSeconds(2)));
    }

    @Test void alwaysKeepsAtLeastOneImage() {
        Sighting value = sighting();
        assertThrows(IllegalStateException.class, () -> value.removeImage(value.images().getFirst().id(), NOW));
    }

    private static Sighting sighting() {
        return Sighting.create(UUID.randomUUID(), UUID.randomUUID(), Species.DOG, "Collar azul",
                NOW.minusSeconds(300), new GeoPoint(4.5339, -75.6811),
                new AdministrativeLocation("63","63001","Granada"),
                List.of("sightings/users/u/1.jpg"), NOW);
    }
}
