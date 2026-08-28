package api

import "testing"

// Spec da issue M1 "Decks CRUD" — contrato em api/openapi.yaml + docs/sync-protocol.md §2.
// Formatos esperados:
//   deck JSON: {"id": "<uuid>", "name": "...", "createdAt": <ms>, "updatedAt": <ms>, "deleted": false}
//   POST /v1/decks {"name": "..."}            -> 201 + deck
//   GET /v1/decks                              -> 200 + lista (sem tombstones)
//   GET /v1/decks/{id}                         -> 200 | 404 (inclusive para tombstone)
//   PUT /v1/decks/{id} {"name": "..."}         -> 200 + deck atualizado (updatedAt novo)
//   DELETE /v1/decks/{id}                      -> 204 (tombstone, nunca apaga a linha)

type deckJSON struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	CreatedAt int64  `json:"createdAt"`
	UpdatedAt int64  `json:"updatedAt"`
	Deleted   bool   `json:"deleted"`
}

func TestDeckCreateAndGet(t *testing.T) {
	t.Skip("un-skip na issue M1: Decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)

	rec := doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "Go"})
	wantStatus(t, rec, 201)
	created := decode[deckJSON](t, rec)
	if created.ID == "" || created.Name != "Go" {
		t.Fatalf("deck criado inválido: %+v", created)
	}

	rec = doJSON(t, s, "GET", "/v1/decks/"+created.ID, specToken, nil)
	wantStatus(t, rec, 200)
}

func TestDeckCreateRejectsBlankName(t *testing.T) {
	t.Skip("un-skip na issue M1: Decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "  "})
	wantStatus(t, rec, 422)
}

func TestDeckListExcludesTombstones(t *testing.T) {
	t.Skip("un-skip na issue M1: Decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)

	a := decode[deckJSON](t, doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "A"}))
	_ = decode[deckJSON](t, doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "B"}))

	wantStatus(t, doJSON(t, s, "DELETE", "/v1/decks/"+a.ID, specToken, nil), 204)

	list := decode[[]deckJSON](t, doJSON(t, s, "GET", "/v1/decks", specToken, nil))
	if len(list) != 1 || list[0].Name != "B" {
		t.Fatalf("lista deveria conter só B, veio: %+v", list)
	}

	// Tombstone: GET individual retorna 404, mas a linha continua no banco
	// (o sync do M2 precisa dela para propagar a deleção).
	wantStatus(t, doJSON(t, s, "GET", "/v1/decks/"+a.ID, specToken, nil), 404)
}

func TestDeckRenameBumpsUpdatedAt(t *testing.T) {
	t.Skip("un-skip na issue M1: Decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)

	created := decode[deckJSON](t, doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "old"}))
	rec := doJSON(t, s, "PUT", "/v1/decks/"+created.ID, specToken, map[string]string{"name": "new"})
	wantStatus(t, rec, 200)
	updated := decode[deckJSON](t, rec)
	if updated.Name != "new" || updated.UpdatedAt < created.UpdatedAt {
		t.Fatalf("rename não refletido: %+v (antes %+v)", updated, created)
	}
}

func TestDeckNotFound(t *testing.T) {
	t.Skip("un-skip na issue M1: Decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	wantStatus(t, doJSON(t, s, "GET", "/v1/decks/00000000-0000-0000-0000-0000000000ff", specToken, nil), 404)
}
