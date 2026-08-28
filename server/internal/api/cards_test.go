package api

import "testing"

// Spec for the M1 issue "Cards CRUD" — contract in api/openapi.yaml and
// docs/sync-protocol.md §2.
//
//	card JSON: {"id","deckId","front","back","frontSrc","backSrc","state","stepIndex",
//	            "intervalDays","easeFactor","repetitions","lapses","dueAt",
//	            "createdAt","updatedAt","deleted"}
//	POST /v1/decks/{id}/cards  -> 201 | 404 (missing/tombstoned deck) | 422 (blank front)
//	GET  /v1/decks/{id}/cards  -> 200, list without tombstones
//	GET/PUT/DELETE /v1/cards/{id} behave like decks.

type cardJSON struct {
	ID        string `json:"id"`
	DeckID    string `json:"deckId"`
	Front     string `json:"front"`
	Back      string `json:"back"`
	State     int    `json:"state"`
	UpdatedAt int64  `json:"updatedAt"`
	Deleted   bool   `json:"deleted"`
}

func specDeck(t *testing.T, s *Server, name string) deckJSON {
	t.Helper()
	rec := doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": name})
	wantStatus(t, rec, 201)
	return decode[deckJSON](t, rec)
}

func TestCardCreateListDelete(t *testing.T) {
	t.Skip("un-skip in M1 issue: cards CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	deck := specDeck(t, s, "Go")

	rec := doJSON(t, s, "POST", "/v1/decks/"+deck.ID+"/cards", specToken,
		map[string]any{"front": "what is a goroutine?", "back": "a lightweight runtime thread"})
	wantStatus(t, rec, 201)
	card := decode[cardJSON](t, rec)
	if card.DeckID != deck.ID || card.State != 0 {
		t.Fatalf("invalid created card (state must start as NEW=0): %+v", card)
	}

	list := decode[[]cardJSON](t, doJSON(t, s, "GET", "/v1/decks/"+deck.ID+"/cards", specToken, nil))
	if len(list) != 1 {
		t.Fatalf("list = %d cards, want 1", len(list))
	}

	wantStatus(t, doJSON(t, s, "DELETE", "/v1/cards/"+card.ID, specToken, nil), 204)
	list = decode[[]cardJSON](t, doJSON(t, s, "GET", "/v1/decks/"+deck.ID+"/cards", specToken, nil))
	if len(list) != 0 {
		t.Fatalf("tombstone should not show up in the list: %+v", list)
	}
}

func TestCardCreateInMissingDeckIs404(t *testing.T) {
	t.Skip("un-skip in M1 issue: cards CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "POST", "/v1/decks/00000000-0000-0000-0000-0000000000ff/cards",
		specToken, map[string]any{"front": "x", "back": "y"})
	wantStatus(t, rec, 404)
}

func TestCardCreateRejectsBlankFront(t *testing.T) {
	t.Skip("un-skip in M1 issue: cards CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	deck := specDeck(t, s, "Go")
	rec := doJSON(t, s, "POST", "/v1/decks/"+deck.ID+"/cards", specToken,
		map[string]any{"front": "", "back": "y"})
	wantStatus(t, rec, 422)
}
