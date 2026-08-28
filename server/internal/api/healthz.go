package api

import (
	"context"
	"net/http"
	"time"
)

func (s *Server) handleHealthz(w http.ResponseWriter, r *http.Request) {
	db := "down"
	if s.st != nil {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()
		if err := s.st.Pool.Ping(ctx); err == nil {
			db = "ok"
		}
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok", "db": db})
}
