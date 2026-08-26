package io.github.KevinMitsi.animalesperdidos.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdministrativeLocationTest {
    @Test
    void createsAndNormalizesAValidLocationWithoutChangingCaseOrAccents() {
        var location = new AdministrativeLocation("63", "63001", "  La   Cañada\tAlta  ");
        assertEquals("63", location.departmentCode());
        assertEquals("63001", location.municipalityCode());
        assertEquals("La Cañada Alta", location.neighborhood());
    }

    @Test void rejectsInvalidDepartmentCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdministrativeLocation("6A", "63001", "Granada"));
    }

    @Test void rejectsInvalidMunicipalityCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdministrativeLocation("63", "6301", "Granada"));
    }

    @Test void rejectsMunicipalityFromAnotherDepartment() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> new AdministrativeLocation("63", "05001", "Granada"));
        assertTrue(error.getMessage().contains("does not belong"));
    }

    @Test void rejectsBlankAndOversizedNeighborhoods() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdministrativeLocation("63", "63001", "   "));
        assertThrows(IllegalArgumentException.class,
                () -> new AdministrativeLocation("63", "63001", "x".repeat(121)));
    }
}
