ALTER TABLE app_user
    ALTER COLUMN password_hash DROP NOT NULL,
    ALTER COLUMN phone DROP NOT NULL,
    ALTER COLUMN document_number DROP NOT NULL,
    ADD COLUMN google_subject VARCHAR(255),
    ADD COLUMN picture_url VARCHAR(2048);

CREATE UNIQUE INDEX uk_app_user_google_subject
    ON app_user (google_subject)
    WHERE google_subject IS NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_authentication_method
        CHECK (password_hash IS NOT NULL OR google_subject IS NOT NULL);

COMMENT ON COLUMN app_user.google_subject IS 'Stable Google OpenID Connect subject; never use email as provider identity';
COMMENT ON COLUMN app_user.picture_url IS 'Profile picture supplied by Google; treated as untrusted display data';
