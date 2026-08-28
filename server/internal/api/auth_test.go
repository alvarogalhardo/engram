package api

import "testing"

// Spec for the M1 issue "Static bearer-token auth middleware".
// Done when: the t.Skip lines are gone and these pass with -race.

func TestAuthRejectsMissingToken(t *testing.T) {
	t.Skip("un-skip in M1 issue: bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/v1/decks", "", nil)
	wantStatus(t, rec, 401)
}

func TestAuthRejectsWrongToken(t *testing.T) {
	t.Skip("un-skip in M1 issue: bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/v1/decks", "wrong-token", nil)
	wantStatus(t, rec, 401)
}

func TestAuthAcceptsConfiguredToken(t *testing.T) {
	t.Skip("un-skip in M1 issue: bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/v1/decks", specToken, nil)
	wantStatus(t, rec, 200)
}

func TestHealthzStaysPublic(t *testing.T) {
	t.Skip("un-skip in M1 issue: bearer auth middleware (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "GET", "/healthz", "", nil)
	wantStatus(t, rec, 200)
}
