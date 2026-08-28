// Package api monta o servidor HTTP com o ServeMux da stdlib (Go 1.22+,
// routing por método e padrão de path — ver ADR-0002).
package api

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/alvarogalhardo/engram/server/internal/config"
	"github.com/alvarogalhardo/engram/server/internal/store"
)

// Server agrupa as dependências dos handlers HTTP.
type Server struct {
	cfg config.Config
	// st é nil quando o banco está indisponível (modo degradado).
	st  *store.Store
	mux *http.ServeMux
}

// New monta o servidor com as rotas registradas.
func New(cfg config.Config, st *store.Store) *Server {
	s := &Server{cfg: cfg, st: st, mux: http.NewServeMux()}
	s.routes()
	return s
}

// Handler devolve o handler raiz com os middlewares aplicados.
func (s *Server) Handler() http.Handler {
	return withLogging(s.mux)
}

func (s *Server) routes() {
	s.mux.HandleFunc("GET /healthz", s.handleHealthz)

	// M1: as rotas /v1 (decks, cards) e o middleware de auth são implementados
	// nas issues M1-2..M1-4 — os testes de spec em *_test.go definem o contrato.
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		slog.Error("writeJSON", "err", err)
	}
}
