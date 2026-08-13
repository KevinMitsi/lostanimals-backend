CREATE TABLE sighting (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES app_user(id),
    species VARCHAR(20) NOT NULL CHECK (species IN ('DOG','CAT','BIRD','OTHER')),
    description VARCHAR(2000) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    location GEOGRAPHY(POINT,4326) NOT NULL,
    neighborhood_id UUID NOT NULL REFERENCES neighborhood(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (observed_at <= created_at)
);

CREATE TABLE sighting_image (
    id UUID PRIMARY KEY,
    sighting_id UUID NOT NULL REFERENCES sighting(id) ON DELETE CASCADE,
    object_key VARCHAR(1024) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    UNIQUE(sighting_id, sort_order)
);

CREATE UNIQUE INDEX uk_sighting_image_one_primary ON sighting_image(sighting_id) WHERE is_primary;
CREATE INDEX idx_sighting_location_active ON sighting USING GIST(location) WHERE status='ACTIVE';
CREATE INDEX idx_sighting_public_cursor ON sighting(created_at DESC,id DESC);
CREATE INDEX idx_sighting_reporter_cursor ON sighting(reporter_id,created_at DESC,id DESC);
CREATE INDEX idx_sighting_duplicate ON sighting(species,observed_at) WHERE status='ACTIVE';
