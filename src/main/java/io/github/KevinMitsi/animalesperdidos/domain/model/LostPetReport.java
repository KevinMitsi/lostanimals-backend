package io.github.kevinmitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LostPetReport(
        UUID id,
        UUID ownerId,
        String petName,
        Species species,
        String description,
        Instant disappearedAt,
        GeoPoint lastSeenAt,
        UUID neighborhoodId,
        ReportStatus status,
        List<String> imageKeys,
        Instant createdAt
) {
    public LostPetReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(species);
        Objects.requireNonNull(disappearedAt);
        Objects.requireNonNull(lastSeenAt);
        Objects.requireNonNull(neighborhoodId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        petName = requireText(petName, "petName");
        description = requireText(description, "description");
        imageKeys = List.copyOf(imageKeys);
        if (disappearedAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Disappearance time cannot be in the future");
        }
        if (imageKeys.isEmpty() || imageKeys.size() > 5) {
            throw new IllegalArgumentException("A report must contain between 1 and 5 images");
        }
    }

    public static LostPetReport create(UUID id, UUID ownerId, String petName, Species species,
                                       String description, Instant disappearedAt, GeoPoint lastSeenAt,
                                       UUID neighborhoodId, List<String> imageKeys, Instant now) {
        return new LostPetReport(id, ownerId, petName, species, description, disappearedAt,
                lastSeenAt, neighborhoodId, ReportStatus.LOST, imageKeys, now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
