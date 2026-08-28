CREATE TABLE users (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Usuário system único: dono de todos os dados até o M3 (contas reais).
-- O id é referenciado por store.SystemUserID.
INSERT INTO users (id) VALUES ('00000000-0000-0000-0000-000000000001');
