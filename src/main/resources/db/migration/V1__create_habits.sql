CREATE TABLE habits (
    id             UUID PRIMARY KEY,
    owner_type     TEXT NOT NULL CHECK (owner_type IN ('USER', 'GROUP')),
    owner_id       UUID NOT NULL,
    name           TEXT NOT NULL,
    schedule_type  TEXT NOT NULL CHECK (schedule_type IN ('DAILY', 'N_TIMES_PER_WEEK', 'SPECIFIC_WEEKDAYS')),
    schedule_params JSONB NOT NULL,
    archived_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL
);

-- Serves: "list a user's (or group's) active habits" — the main lookup
-- pattern once slice 4 needs a group's habits, and useful now for a user's own.
CREATE INDEX idx_habits_owner ON habits (owner_type, owner_id);
