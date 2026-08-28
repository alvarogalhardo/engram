// Package api wires the HTTP server on the stdlib ServeMux (Go 1.22+,
// method and path-pattern routing — see ADR-0002).
package api

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/alvarogalhardo/engram/server/internal/config"
	"github.com/alvarogalhardo/engram/server/internal/store"
)

// Server groups the dependencies shared by the HTTP handlers.
type Server struct {
	cfg config.Config
	// st is nil when the database is unavailable (degraded mode).
	st  *store.Store
	mux *http.ServeMux
}

// New builds the server with its routes registered.
func New(cfg config.Config, st *store.Store) *Server {
	s := &Server{cfg: cfg, st: st, mux: http.NewServeMux()}
	s.routes()
	return s
}

// Handler returns the root handler with middleware applied.
func (s *Server) Handler() http.Handler {
	return withLogging(s.mux)
}

func (s *Server) routes() {
	s.mux.HandleFunc("GET /healthz", s.handleHealthz)

	// M1: the /v1 routes (decks, cards) and the auth middleware are implemented
	// in issues M1-2..M1-4 — the spec tests in *_test.go define the contract.
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		slog.Error("writeJSON", "err", err)
	}
}
