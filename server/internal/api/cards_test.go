package api

import "testing"

// Spec da issue M1 "Cards CRUD" — contrato em api/openapi.yaml + docs/sync-protocol.md §2.
//   card JSON: {"id","deckId","front","back","frontSrc","backSrc","state","stepIndex",
//               "intervalDays","easeFactor","repetitions","lapses","dueAt",
//               "createdAt","updatedAt","deleted"}
//   POST /v1/decks/{id}/cards  -> 201 | 404 (deck inexistente/tombstone) | 422 (front vazio)
//   GET  /v1/decks/{id}/cards  -> 200 lista sem tombstones
//   GET/PUT/DELETE /v1/cards/{id} como em decks.

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
	t.Skip("un-skip na issue M1: Cards CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	deck := specDeck(t, s, "Go")

	rec := doJSON(t, s, "POST", "/v1/decks/"+deck.ID+"/cards", specToken,
		map[string]any{"front": "o que é goroutine?", "back": "thread leve do runtime"})
	wantStatus(t, rec, 201)
	card := decode[cardJSON](t, rec)
	if card.DeckID != deck.ID || card.State != 0 {
		t.Fatalf("card criado inválido (state deve nascer NEW=0): %+v", card)
	}

	list := decode[[]cardJSON](t, doJSON(t, s, "GET", "/v1/decks/"+deck.ID+"/cards", specToken, nil))
	if len(list) != 1 {
		t.Fatalf("lista = %d cards, want 1", len(list))
	}

	wantStatus(t, doJSON(t, s, "DELETE", "/v1/cards/"+card.ID, specToken, nil), 204)
	list = decode[[]cardJSON](t, doJSON(t, s, "GET", "/v1/decks/"+deck.ID+"/cards", specToken, nil))
	if len(list) != 0 {
		t.Fatalf("tombstone não deveria aparecer na lista: %+v", list)
	}
}

func TestCardCreateInMissingDeckIs404(t *testing.T) {
	t.Skip("un-skip na issue M1: Cards CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "POST", "/v1/decks/00000000-0000-0000-0000-0000000000ff/cards",
		specToken, map[string]any{"front": "x", "back": "y"})
	wantStatus(t, rec, 404)
}

func TestCardCreateRejectsBlankFront(t *testing.T) {
	t.Skip("un-skip na issue M1: Cards CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	deck := specDeck(t, s, "Go")
	rec := doJSON(t, s, "POST", "/v1/decks/"+deck.ID+"/cards", specToken,
		map[string]any{"front": "", "back": "y"})
	wantStatus(t, rec, 422)
}
