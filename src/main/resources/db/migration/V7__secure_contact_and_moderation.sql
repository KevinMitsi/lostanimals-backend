ALTER TABLE app_user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
    CHECK (role IN ('USER','MODERATOR','ADMIN'));

CREATE TABLE service_area (
    city_id UUID PRIMARY KEY REFERENCES city(id),
    enabled BOOLEAN NOT NULL,
    updated_by UUID REFERENCES app_user(id),
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO service_area(city_id,enabled,updated_at)
VALUES ('a2000000-0000-0000-0000-000000000001',true,now());

CREATE TABLE contact_request (
    id UUID PRIMARY KEY,
    publication_type VARCHAR(20) NOT NULL CHECK (publication_type IN ('LOST_PET_REPORT','SIGHTING')),
    publication_id UUID NOT NULL,
    requester_id UUID NOT NULL REFERENCES app_user(id),
    recipient_id UUID NOT NULL REFERENCES app_user(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELED')),
    note VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    answered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (requester_id <> recipient_id)
);
CREATE UNIQUE INDEX uk_contact_request_pending
    ON contact_request(publication_type,publication_id,requester_id) WHERE status='PENDING';
CREATE INDEX idx_contact_request_recipient ON contact_request(recipient_id,status,created_at DESC,id DESC);
CREATE INDEX idx_contact_request_requester ON contact_request(requester_id,created_at DESC,id DESC);

CREATE TABLE conversation (
    id UUID PRIMARY KEY,
    contact_request_id UUID NOT NULL UNIQUE REFERENCES contact_request(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE conversation_participant (
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    joined_at TIMESTAMPTZ NOT NULL,
    left_at TIMESTAMPTZ,
    PRIMARY KEY(conversation_id,user_id)
);
CREATE INDEX idx_conversation_participant_user ON conversation_participant(user_id,conversation_id);

CREATE TABLE conversation_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES app_user(id),
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_conversation_message_poll
    ON conversation_message(conversation_id,created_at,id);

CREATE TABLE user_block (
    blocker_id UUID NOT NULL REFERENCES app_user(id),
    blocked_id UUID NOT NULL REFERENCES app_user(id),
    conversation_id UUID REFERENCES conversation(id),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(blocker_id,blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE TABLE conversation_report (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    reporter_id UUID NOT NULL REFERENCES app_user(id),
    reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','RESOLVED','DISMISSED')),
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID REFERENCES app_user(id),
    reviewed_at TIMESTAMPTZ,
    UNIQUE(conversation_id,reporter_id)
);
CREATE INDEX idx_conversation_report_moderation ON conversation_report(status,created_at,id);

CREATE TABLE reunion_review (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES lost_pet_report(id),
    requested_by UUID NOT NULL REFERENCES app_user(id),
    request_note VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID REFERENCES app_user(id),
    review_note VARCHAR(1000),
    reviewed_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uk_reunion_review_pending ON reunion_review(report_id) WHERE status='PENDING';
CREATE INDEX idx_reunion_review_queue ON reunion_review(status,created_at,id);
