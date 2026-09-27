-- Shareable invite links. Multi-use until they expire; the token is the
-- secret, so it is only ever looked up by primary key (no other index needed).
CREATE TABLE group_invites (
    token       TEXT PRIMARY KEY,
    group_id    UUID NOT NULL REFERENCES groups (id),
    created_by  UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL
);
