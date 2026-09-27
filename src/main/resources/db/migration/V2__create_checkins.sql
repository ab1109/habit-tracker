CREATE TABLE checkins (
    id                    UUID PRIMARY KEY,
    habit_id              UUID NOT NULL REFERENCES habits (id),
    group_id              UUID,
    user_id               UUID NOT NULL,
    performed_by_user_id  UUID NOT NULL,
    recorded_at           TIMESTAMPTZ NOT NULL,
    local_date            DATE NOT NULL
);

-- Individual habit: at most one check-in per habit, user, and local date.
-- Slice 2 only ever inserts rows with group_id NULL, so this is the index
-- that's actually exercised right now.
CREATE UNIQUE INDEX uq_checkins_individual
    ON checkins (habit_id, user_id, local_date)
    WHERE group_id IS NULL;

-- Joint group habit (slice 4): at most one check-in per habit, group, and
-- local date, regardless of which member performed it.
CREATE UNIQUE INDEX uq_checkins_group
    ON checkins (habit_id, group_id, local_date)
    WHERE group_id IS NOT NULL;

-- Serves: "list a user's check-ins for a habit in a date range" (slice 3's
-- streak calculation reads exactly this).
CREATE INDEX idx_checkins_habit_user_date ON checkins (habit_id, user_id, local_date);
