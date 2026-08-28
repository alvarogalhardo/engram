// Package migrations embeds the .sql files into the binary so the server can
// apply migrations by itself on startup (no external CLI needed).
package migrations

import "embed"

// FS exposes the embedded migration files for the store to apply on startup.
//
//go:embed *.sql
var FS embed.FS
