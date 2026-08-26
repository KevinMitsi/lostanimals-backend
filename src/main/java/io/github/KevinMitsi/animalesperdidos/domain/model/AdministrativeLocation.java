package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record AdministrativeLocation(String departmentCode, String municipalityCode, String neighborhood) {
    private static final Pattern DEPARTMENT_CODE = Pattern.compile("^[0-9]{2}$");
    private static final Pattern MUNICIPALITY_CODE = Pattern.compile("^[0-9]{5}$");
    private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");

    public AdministrativeLocation {
        Objects.requireNonNull(departmentCode, "departmentCode is required");
        Objects.requireNonNull(municipalityCode, "municipalityCode is required");
        if (!DEPARTMENT_CODE.matcher(departmentCode).matches()) {
            throw new IllegalArgumentException("departmentCode must contain exactly 2 digits");
        }
        if (!MUNICIPALITY_CODE.matcher(municipalityCode).matches()) {
            throw new IllegalArgumentException("municipalityCode must contain exactly 5 digits");
        }
        if (!municipalityCode.startsWith(departmentCode)) {
            throw new IllegalArgumentException("municipalityCode does not belong to departmentCode");
        }
        neighborhood = normalizeNeighborhood(neighborhood);
        if (neighborhood.length() > 120) {
            throw new IllegalArgumentException("neighborhood cannot exceed 120 characters");
        }
    }

    public static String normalizeNeighborhood(String value) {
        Objects.requireNonNull(value, "neighborhood is required");
        String normalized = REPEATED_WHITESPACE.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty()) throw new IllegalArgumentException("neighborhood is required");
        return normalized;
    }

    public static void validateDepartmentCode(String value) {
        if (value == null || !DEPARTMENT_CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("departmentCode must contain exactly 2 digits");
        }
    }

    public static void validateMunicipalityCode(String value) {
        if (value == null || !MUNICIPALITY_CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("municipalityCode must contain exactly 5 digits");
        }
    }
}
