package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LostPetImage(UUID id, String objectKey, boolean primary, int sortOrder) {
    public LostPetImage {
        Objects.requireNonNull(id);
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey is required");
        if (sortOrder < 0) throw new IllegalArgumentException("sortOrder cannot be negative");
    }
}
