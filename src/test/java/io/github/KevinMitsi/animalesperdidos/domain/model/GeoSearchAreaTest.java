package io.github.KevinMitsi.animalesperdidos.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoSearchAreaTest {
    @Test void acceptsSupportedRadiusBoundaries() {
        assertEquals(100, new GeoSearchArea(new GeoPoint(4.53, -75.68), 100).radiusMeters());
        assertEquals(50_000, new GeoSearchArea(new GeoPoint(4.53, -75.68), 50_000).radiusMeters());
    }

    @Test void rejectsRadiusOutsideTheSupportedArea() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoSearchArea(new GeoPoint(4.53, -75.68), 50_001));
    }
}
