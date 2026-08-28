// Package store é a camada de acesso ao Postgres: pool pgx, migrations
// embarcadas aplicadas no startup e, conforme os milestones avançam, as
// queries (SQL escrito à mão até o M4, quando o sqlc entra).
package store

// SystemUserID é o usuário único seedado pela migration 0001, dono de todos os
// dados até o M3 introduzir contas reais (ver ADR-0005).
const SystemUserID = "00000000-0000-0000-0000-000000000001"
