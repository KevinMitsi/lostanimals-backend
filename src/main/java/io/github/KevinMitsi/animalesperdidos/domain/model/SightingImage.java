package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SightingImage(UUID id, String objectKey, boolean primary, int sortOrder) {
    public SightingImage {
        Objects.requireNonNull(id);
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey is required");
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder cannot be negative");
    }
}
