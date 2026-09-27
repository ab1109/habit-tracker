CREATE TABLE groups (
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL,
    created_by  UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE group_members (
    group_id      UUID NOT NULL REFERENCES groups (id),
    user_id       UUID NOT NULL,
    display_name  TEXT NOT NULL,
    joined_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

-- Serves: "which circles is this user in" (GET /groups) and every
-- membership check made by user id.
CREATE INDEX idx_group_members_user ON group_members (user_id);

-- A member's personal habit shared with a circle for visibility only: the
-- habit stays USER-owned and its check-ins stay individual.
CREATE TABLE group_shared_habits (
    group_id           UUID NOT NULL REFERENCES groups (id),
    habit_id           UUID NOT NULL REFERENCES habits (id),
    shared_by_user_id  UUID NOT NULL,
    shared_at          TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (group_id, habit_id)
);

-- Serves: "is this habit shared with a circle the viewer belongs to" — the
-- access check on a shared habit's progress, which looks up by habit.
CREATE INDEX idx_group_shared_habits_habit ON group_shared_habits (habit_id);
