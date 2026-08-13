package io.github.KevinMitsi.animalesperdidos.domain.model;

public record GeoSearchArea(GeoPoint center, double radiusMeters) {
    public static final double MIN_RADIUS_METERS = 100;
    public static final double MAX_RADIUS_METERS = 50_000;

    public GeoSearchArea {
        if (center == null) throw new IllegalArgumentException("Search center is required");
        if (!Double.isFinite(radiusMeters) || radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("Search radius must be between 100 and 50000 meters");
        }
    }
}
