// Package sync will hold the synchronization logic of milestone M2
// (LWW push, pull by server_seq cursor, append-only review upload).
//
// The specification this package implements lives in docs/sync-protocol.md;
// the (skipped) spec tests are in internal/api/sync_test.go.
package sync
