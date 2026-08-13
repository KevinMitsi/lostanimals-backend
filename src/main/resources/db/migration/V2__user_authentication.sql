ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100) NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT uk_app_user_phone UNIQUE (phone);

CREATE UNIQUE INDEX uk_app_user_email_normalized ON app_user (lower(email));

COMMENT ON COLUMN app_user.password_hash IS 'Adaptive password hash only; raw passwords are never persisted';
