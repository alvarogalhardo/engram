// Package sync abrigará a lógica de sincronização do milestone M2
// (push LWW, pull por cursor server_seq, upload append-only de reviews).
//
// A especificação que este pacote implementa vive em docs/sync-protocol.md;
// os testes de spec (pulados) estão em internal/api/sync_test.go.
package sync
