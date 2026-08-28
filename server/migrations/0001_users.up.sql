CREATE TABLE users (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Single system user: owns all data until M3 introduces real accounts.
-- This id is referenced by store.SystemUserID.
INSERT INTO users (id) VALUES ('00000000-0000-0000-0000-000000000001');
