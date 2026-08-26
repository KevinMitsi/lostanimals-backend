package io.github.KevinMitsi.animalesperdidos.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

public record Sighting(UUID id, UUID reporterId, Species species, String description, Instant observedAt,
                       GeoPoint location, AdministrativeLocation administrativeLocation, SightingStatus status,
                       List<SightingImage> images, Instant createdAt, Instant updatedAt, long version) {
    public Sighting {
        Objects.requireNonNull(id); Objects.requireNonNull(reporterId); Objects.requireNonNull(species);
        Objects.requireNonNull(observedAt); Objects.requireNonNull(location); Objects.requireNonNull(administrativeLocation);
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        description = description.trim();
        if (description.length() > 2000) throw new IllegalArgumentException("description cannot exceed 2000 characters");
        images = List.copyOf(images);
        if (observedAt.isAfter(createdAt)) throw new IllegalArgumentException("Observation cannot be in the future");
        if (images.isEmpty() || images.size() > 5) throw new IllegalArgumentException("A sighting needs 1 to 5 images");
        if (images.stream().filter(SightingImage::primary).count() != 1) throw new IllegalArgumentException("Exactly one image must be primary");
        if (images.stream().map(SightingImage::objectKey).distinct().count() != images.size())
            throw new IllegalArgumentException("Image keys must be unique");
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }

    public static Sighting create(UUID id, UUID reporterId, Species species, String description, Instant observedAt,
                                  GeoPoint location, AdministrativeLocation administrativeLocation, List<String> keys, Instant now) {
        List<SightingImage> images = IntStream.range(0, keys.size())
                .mapToObj(i -> new SightingImage(UUID.randomUUID(), keys.get(i), i == 0, i)).toList();
        return new Sighting(id, reporterId, species, description, observedAt, location, administrativeLocation,
                SightingStatus.ACTIVE, images, now, now, 0);
    }

    public Sighting edit(Species species, String description, Instant observedAt, GeoPoint location,
                         AdministrativeLocation administrativeLocation, Instant now) {
        requireActive();
        return new Sighting(id, reporterId, species, description, observedAt, location, administrativeLocation,
                status, images, createdAt, now, version);
    }

    public Sighting close(Instant now) {
        requireActive();
        return new Sighting(id, reporterId, species, description, observedAt, location, administrativeLocation,
                SightingStatus.CLOSED, images, createdAt, now, version);
    }

    public Sighting addImage(SightingImage image, Instant now) {
        requireActive();
        if (images.size() >= 5) throw new IllegalStateException("A sighting cannot have more than 5 images");
        var copy = new java.util.ArrayList<>(images);
        copy.add(new SightingImage(image.id(), image.objectKey(), false, images.size()));
        return withImages(copy, now);
    }

    public Sighting removeImage(UUID imageId, Instant now) {
        requireActive();
        if (images.size() == 1) throw new IllegalStateException("A sighting must keep one image");
        SightingImage removed = image(imageId);
        List<SightingImage> remaining = images.stream().filter(i -> !i.id().equals(imageId)).toList();
        UUID primary = removed.primary() ? remaining.getFirst().id()
                : remaining.stream().filter(SightingImage::primary).findFirst().orElseThrow().id();
        return withImages(normalize(remaining, primary), now);
    }

    public Sighting setPrimary(UUID imageId, Instant now) {
        requireActive(); image(imageId);
        return withImages(normalize(images, imageId), now);
    }

    public SightingImage image(UUID id) {
        return images.stream().filter(i -> i.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
    }

    private Sighting withImages(List<SightingImage> value, Instant now) {
        return new Sighting(id, reporterId, species, description, observedAt, location, administrativeLocation,
                status, value, createdAt, now, version);
    }
    private static List<SightingImage> normalize(List<SightingImage> source, UUID primary) {
        return IntStream.range(0, source.size()).mapToObj(i -> new SightingImage(source.get(i).id(),
                source.get(i).objectKey(), source.get(i).id().equals(primary), i)).toList();
    }
    private void requireActive() { if (status != SightingStatus.ACTIVE) throw new IllegalStateException("Sighting is closed"); }
}
