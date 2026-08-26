package io.github.KevinMitsi.animalesperdidos.application.service;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.domain.model.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

final class SearchCriteriaPolicy {
    private SearchCriteriaPolicy() { }

    static GeoSearchArea area(Double latitude, Double longitude, Double radiusMeters) {
        boolean none = latitude == null && longitude == null && radiusMeters == null;
        if (none) return null;
        if (latitude == null || longitude == null || radiusMeters == null) {
            throw new BusinessRuleViolation("latitude, longitude and radiusMeters must be provided together");
        }
        try { return new GeoSearchArea(new GeoPoint(latitude, longitude), radiusMeters); }
        catch (IllegalArgumentException error) { throw new BusinessRuleViolation(error.getMessage()); }
    }

    static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolation("from must be before or equal to to");
        }
    }

    static String validateLocationFilters(String departmentCode, String municipalityCode, String neighborhood) {
        if (departmentCode != null) AdministrativeLocation.validateDepartmentCode(departmentCode);
        if (municipalityCode != null) AdministrativeLocation.validateMunicipalityCode(municipalityCode);
        if (departmentCode != null && municipalityCode != null && !municipalityCode.startsWith(departmentCode)) {
            throw new IllegalArgumentException("municipalityCode does not belong to departmentCode");
        }
        return neighborhood == null ? null : AdministrativeLocation.normalizeNeighborhood(neighborhood);
    }

    static Cursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return new Cursor(null, null);
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] values = raw.split("\\|", -1);
            if (values.length != 2) throw new IllegalArgumentException();
            return new Cursor(Instant.parse(values[0]), UUID.fromString(values[1]));
        } catch (RuntimeException error) {
            throw new BusinessRuleViolation("Invalid pagination cursor");
        }
    }

    static String encode(Instant createdAt, UUID id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (createdAt + "|" + id).getBytes(StandardCharsets.UTF_8));
    }

    record Cursor(Instant createdAt, UUID id) { }
}
