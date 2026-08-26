package io.github.KevinMitsi.animalesperdidos;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MigrationV10Test {
    @Test
    void migrationBackfillsArmeniaAndProtectsHistoricalPublications() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V10__migrate_to_divipola_administrative_locations.sql")) {
            assertNotNull(stream);
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("department_code = '63'"));
            assertTrue(sql.contains("municipality_code = '63001'"));
            assertTrue(sql.contains("JOIN neighborhood"));
            assertTrue(sql.contains("backfill left lost_pet_report"));
            assertTrue(sql.contains("backfill left sighting"));
            assertFalse(sql.matches("(?is).*DELETE\\s+FROM\\s+(lost_pet_report|sighting).*"));
        }
    }
}
