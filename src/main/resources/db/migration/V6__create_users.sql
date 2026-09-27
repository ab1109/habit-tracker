-- Accounts for signed-in users (Google). In local dev-header mode users are
-- identified by the X-User-Id header alone and have no row here.
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    google_subject  TEXT NOT NULL,
    email           TEXT,
    display_name    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

-- Serves: resolving the signed-in Google account to our user id, on every
-- request; also guarantees one account per Google identity.
CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject);
