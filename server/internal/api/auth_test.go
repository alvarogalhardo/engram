package api

import "testing"

// Spec da issue M1 "Bearer auth middleware".
// Definição de pronto: remover os t.Skip e passar com -race.

func TestAuthRejectsMissingToken(t *testing.T) {
	t.Skip("un-skip na issue M1: Bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/v1/decks", "", nil)
	wantStatus(t, rec, 401)
}

func TestAuthRejectsWrongToken(t *testing.T) {
	t.Skip("un-skip na issue M1: Bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/v1/decks", "wrong-token", nil)
	wantStatus(t, rec, 401)
}

func TestAuthAcceptsConfiguredToken(t *testing.T) {
	t.Skip("un-skip na issue M1: Bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/v1/decks", specToken, nil)
	wantStatus(t, rec, 200)
}

func TestHealthzStaysPublic(t *testing.T) {
	t.Skip("un-skip na issue M1: Bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/healthz", "", nil)
	wantStatus(t, rec, 200)
}
