CREATE TABLE IF NOT EXISTS notification_outbox (
    id UUID PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    status TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_status_created_at
    ON notification_outbox(status, created_at);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_aggregate_id
    ON notification_outbox(aggregate_id);
