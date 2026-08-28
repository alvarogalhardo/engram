package api

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/alvarogalhardo/engram/server/internal/config"
	"github.com/alvarogalhardo/engram/server/internal/store"
)

// Helpers for the spec tests (M1/M2). These run against a real Postgres:
// export DATABASE_URL (docker compose up postgres) and remove the t.Skip of
// whichever issue you are implementing.

const specToken = "spec-test-token"

func newSpecServer(t *testing.T) *Server {
	t.Helper()
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		t.Skip("DATABASE_URL not set — the spec tests need Postgres")
	}
	st, err := store.Open(context.Background(), dbURL)
	if err != nil {
		t.Fatalf("opening store: %v", err)
	}
	t.Cleanup(st.Close)
	if err := st.Migrate(); err != nil {
		t.Fatalf("migrations: %v", err)
	}
	// Wipe data between tests; the error is ignored while the tables from
	// migration 0002 (issue M1) do not exist yet.
	_, _ = st.Pool.Exec(context.Background(),
		`TRUNCATE review_logs, cards, decks RESTART IDENTITY CASCADE`)
	return New(config.Config{Port: "0", DatabaseURL: dbURL, APIToken: specToken}, st)
}

func doJSON(t *testing.T, s *Server, method, path, token string, body any) *httptest.ResponseRecorder {
	t.Helper()
	var buf bytes.Buffer
	if body != nil {
		if err := json.NewEncoder(&buf).Encode(body); err != nil {
			t.Fatalf("encode body: %v", err)
		}
	}
	req := httptest.NewRequest(method, path, &buf)
	req.Header.Set("Content-Type", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	rec := httptest.NewRecorder()
	s.Handler().ServeHTTP(rec, req)
	return rec
}

func decode[T any](t *testing.T, rec *httptest.ResponseRecorder) T {
	t.Helper()
	var v T
	if err := json.Unmarshal(rec.Body.Bytes(), &v); err != nil {
		t.Fatalf("response is not valid JSON (%d): %s", rec.Code, rec.Body.String())
	}
	return v
}

func wantStatus(t *testing.T, rec *httptest.ResponseRecorder, want int) {
	t.Helper()
	if rec.Code != want {
		t.Fatalf("status = %d, want %d — body: %s", rec.Code, want, rec.Body.String())
	}
}

var _ = http.StatusOK // keeps the import stable while the tests are skipped
