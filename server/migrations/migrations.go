// Package migrations embarca os arquivos .sql no binário para que o servidor
// aplique as migrations sozinho no startup (sem CLI externa).
package migrations

import "embed"

// FS expõe os arquivos de migration embarcados para o store aplicar no startup.
//
//go:embed *.sql
var FS embed.FS
