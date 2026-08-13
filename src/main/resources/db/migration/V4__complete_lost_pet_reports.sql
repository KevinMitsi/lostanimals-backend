ALTER TABLE lost_pet_report
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE lost_pet_report SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE lost_pet_report ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX idx_lost_pet_report_public_cursor
    ON lost_pet_report(created_at DESC, id DESC);

CREATE INDEX idx_lost_pet_report_owner_cursor
    ON lost_pet_report(owner_id, created_at DESC, id DESC);

CREATE UNIQUE INDEX uk_lost_pet_image_one_primary
    ON lost_pet_image(report_id) WHERE is_primary;
