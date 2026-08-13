CREATE TABLE push_subscription (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    endpoint_arn VARCHAR(2048) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_push_subscription_user_enabled ON push_subscription(user_id) WHERE enabled;

CREATE TABLE notification_delivery (
    event_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK(channel IN ('EMAIL','PUSH')),
    target VARCHAR(2048) NOT NULL,
    delivered_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(event_id,channel,target)
);
