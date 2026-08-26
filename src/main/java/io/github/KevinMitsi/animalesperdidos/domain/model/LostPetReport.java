package io.github.KevinMitsi.animalesperdidos.domain.model;

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
        AdministrativeLocation administrativeLocation,
        ReportStatus status,
        List<LostPetImage> images,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public LostPetReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(species);
        Objects.requireNonNull(disappearedAt);
        Objects.requireNonNull(lastSeenAt);
        Objects.requireNonNull(administrativeLocation);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        petName = requireText(petName, "petName");
        description = requireText(description, "description");
        images = List.copyOf(images);
        if (disappearedAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Disappearance time cannot be in the future");
        }
        if (images.isEmpty() || images.size() > 5) {
            throw new IllegalArgumentException("A report must contain between 1 and 5 images");
        }
        if (images.stream().filter(LostPetImage::primary).count() != 1) {
            throw new IllegalArgumentException("A report must contain exactly one primary image");
        }
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }

    public static LostPetReport create(UUID id, UUID ownerId, String petName, Species species,
                                       String description, Instant disappearedAt, GeoPoint lastSeenAt,
                                       AdministrativeLocation administrativeLocation, List<String> imageKeys, Instant now) {
        List<LostPetImage> images = java.util.stream.IntStream.range(0, imageKeys.size())
                .mapToObj(index -> new LostPetImage(UUID.randomUUID(), imageKeys.get(index), index == 0, index))
                .toList();
        return new LostPetReport(id, ownerId, petName, species, description, disappearedAt,
                lastSeenAt, administrativeLocation, ReportStatus.LOST, images, now, now, 0);
    }

    public LostPetReport edit(String petName, Species species, String description, Instant disappearedAt,
                              GeoPoint lastSeenAt, AdministrativeLocation administrativeLocation, Instant now) {
        if (status != ReportStatus.LOST) throw new IllegalStateException("Only active reports can be edited");
        return new LostPetReport(id, ownerId, petName, species, description, disappearedAt, lastSeenAt,
                administrativeLocation, status, images, createdAt, now, version);
    }

    public LostPetReport changeStatus(ReportStatus newStatus, Instant now) {
        if (status != ReportStatus.LOST) throw new IllegalStateException("A closed report cannot change status");
        if (newStatus == ReportStatus.LOST) throw new IllegalArgumentException("Status must close the report");
        return new LostPetReport(id, ownerId, petName, species, description, disappearedAt, lastSeenAt,
                administrativeLocation, newStatus, images, createdAt, now, version);
    }

    public LostPetReport reopen(Instant now, java.time.Duration allowedWindow) {
        if (status == ReportStatus.LOST) throw new IllegalStateException("Report is already active");
        if (status == ReportStatus.REUNITED) throw new IllegalStateException("A moderator-confirmed reunion cannot be reopened");
        if (updatedAt.plus(allowedWindow).isBefore(now)) {
            throw new IllegalStateException("The reopening window has expired");
        }
        return new LostPetReport(id, ownerId, petName, species, description, disappearedAt, lastSeenAt,
                administrativeLocation, ReportStatus.LOST, images, createdAt, now, version);
    }

    public LostPetReport addImage(LostPetImage image, Instant now) {
        if (status != ReportStatus.LOST) throw new IllegalStateException("Images cannot be changed on a closed report");
        if (images.size() >= 5) throw new IllegalStateException("A report cannot contain more than 5 images");
        List<LostPetImage> updated = new java.util.ArrayList<>(images);
        updated.add(new LostPetImage(image.id(), image.objectKey(), false, images.size()));
        return copyWithImages(updated, now);
    }

    public LostPetReport removeImage(UUID imageId, Instant now) {
        if (status != ReportStatus.LOST) throw new IllegalStateException("Images cannot be changed on a closed report");
        if (images.size() == 1) throw new IllegalStateException("A report must keep at least one image");
        LostPetImage removed = images.stream().filter(image -> image.id().equals(imageId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        List<LostPetImage> remaining = images.stream().filter(image -> !image.id().equals(imageId)).toList();
        UUID primaryId = removed.primary() ? remaining.getFirst().id() :
                remaining.stream().filter(LostPetImage::primary).findFirst().orElseThrow().id();
        return copyWithImages(normalize(remaining, primaryId), now);
    }

    public LostPetReport setPrimaryImage(UUID imageId, Instant now) {
        if (status != ReportStatus.LOST) throw new IllegalStateException("Images cannot be changed on a closed report");
        if (images.stream().noneMatch(image -> image.id().equals(imageId))) throw new IllegalArgumentException("Image not found");
        return copyWithImages(normalize(images, imageId), now);
    }

    public LostPetImage image(UUID imageId) {
        return images.stream().filter(image -> image.id().equals(imageId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
    }

    private LostPetReport copyWithImages(List<LostPetImage> updatedImages, Instant now) {
        return new LostPetReport(id, ownerId, petName, species, description, disappearedAt, lastSeenAt,
                administrativeLocation, status, updatedImages, createdAt, now, version);
    }

    private static List<LostPetImage> normalize(List<LostPetImage> source, UUID primaryId) {
        return java.util.stream.IntStream.range(0, source.size())
                .mapToObj(index -> new LostPetImage(source.get(index).id(), source.get(index).objectKey(),
                        source.get(index).id().equals(primaryId), index)).toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
