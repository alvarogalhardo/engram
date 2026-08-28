package api

import (
	"encoding/json"
	"net/http/httptest"
	"testing"

	"github.com/alvarogalhardo/engram/server/internal/config"
)

// Reference handler test using httptest.
func TestHealthzWithoutDatabase(t *testing.T) {
	srv := New(config.Config{Port: "8080", APIToken: "t"}, nil)

	req := httptest.NewRequest("GET", "/healthz", nil)
	rec := httptest.NewRecorder()
	srv.Handler().ServeHTTP(rec, req)

	if rec.Code != 200 {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	var body map[string]string
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("invalid JSON: %v", err)
	}
	if body["status"] != "ok" || body["db"] != "down" {
		t.Errorf("body = %v, want status=ok db=down", body)
	}
}
