CREATE TABLE notification_preferences (
    user_id                UUID PRIMARY KEY,
    weekly_digest_enabled  BOOLEAN NOT NULL,
    timezone               TEXT NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL
);

-- One row per delivery attempt, successful or not.
CREATE TABLE notification_deliveries (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    group_id      UUID NOT NULL,
    kind          TEXT NOT NULL CHECK (kind IN ('WEEKLY_DIGEST')),
    period_start  DATE NOT NULL,
    status        TEXT NOT NULL CHECK (status IN ('SENT', 'FAILED')),
    subject       TEXT NOT NULL,
    body          TEXT NOT NULL,
    error         TEXT,
    attempted_at  TIMESTAMPTZ NOT NULL
);

-- At most one successful digest per member, circle and week, so the hourly
-- scheduler can re-run (or overlap) without sending twice. Failed attempts
-- are not constrained, so a later run can retry them.
CREATE UNIQUE INDEX uq_notification_deliveries_sent
    ON notification_deliveries (user_id, group_id, kind, period_start)
    WHERE status = 'SENT';

-- Serves: GET /me/notifications — a user's deliveries, newest first.
CREATE INDEX idx_notification_deliveries_user ON notification_deliveries (user_id, attempted_at DESC);
