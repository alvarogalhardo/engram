package api

import "testing"

// Spec for the M1 issue "Decks CRUD" — contract in api/openapi.yaml and
// docs/sync-protocol.md §2. Expected shapes:
//
//	deck JSON: {"id": "<uuid>", "name": "...", "createdAt": <ms>, "updatedAt": <ms>, "deleted": false}
//	POST /v1/decks {"name": "..."}      -> 201 + deck
//	GET /v1/decks                        -> 200 + list (tombstones excluded)
//	GET /v1/decks/{id}                   -> 200 | 404 (404 for tombstones too)
//	PUT /v1/decks/{id} {"name": "..."}   -> 200 + updated deck (fresh updatedAt)
//	DELETE /v1/decks/{id}                -> 204 (tombstone; never deletes the row)

type deckJSON struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	CreatedAt int64  `json:"createdAt"`
	UpdatedAt int64  `json:"updatedAt"`
	Deleted   bool   `json:"deleted"`
}

func TestDeckCreateAndGet(t *testing.T) {
	t.Skip("un-skip in M1 issue: decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)

	rec := doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "Go"})
	wantStatus(t, rec, 201)
	created := decode[deckJSON](t, rec)
	if created.ID == "" || created.Name != "Go" {
		t.Fatalf("invalid created deck: %+v", created)
	}

	rec = doJSON(t, s, "GET", "/v1/decks/"+created.ID, specToken, nil)
	wantStatus(t, rec, 200)
}

func TestDeckCreateRejectsBlankName(t *testing.T) {
	t.Skip("un-skip in M1 issue: decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	rec := doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "  "})
	wantStatus(t, rec, 422)
}

func TestDeckListExcludesTombstones(t *testing.T) {
	t.Skip("un-skip in M1 issue: decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)

	a := decode[deckJSON](t, doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "A"}))
	_ = decode[deckJSON](t, doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "B"}))

	wantStatus(t, doJSON(t, s, "DELETE", "/v1/decks/"+a.ID, specToken, nil), 204)

	list := decode[[]deckJSON](t, doJSON(t, s, "GET", "/v1/decks", specToken, nil))
	if len(list) != 1 || list[0].Name != "B" {
		t.Fatalf("list should contain only B, got: %+v", list)
	}

	// Tombstone: the individual GET returns 404, but the row stays in the
	// database — M2's sync needs it to propagate the deletion.
	wantStatus(t, doJSON(t, s, "GET", "/v1/decks/"+a.ID, specToken, nil), 404)
}

func TestDeckRenameBumpsUpdatedAt(t *testing.T) {
	t.Skip("un-skip in M1 issue: decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)

	created := decode[deckJSON](t, doJSON(t, s, "POST", "/v1/decks", specToken, map[string]string{"name": "old"}))
	rec := doJSON(t, s, "PUT", "/v1/decks/"+created.ID, specToken, map[string]string{"name": "new"})
	wantStatus(t, rec, 200)
	updated := decode[deckJSON](t, rec)
	if updated.Name != "new" || updated.UpdatedAt < created.UpdatedAt {
		t.Fatalf("rename not reflected: %+v (was %+v)", updated, created)
	}
}

func TestDeckNotFound(t *testing.T) {
	t.Skip("un-skip in M1 issue: decks CRUD (docs/roadmap.md)")
	s := newSpecServer(t)
	wantStatus(t, doJSON(t, s, "GET", "/v1/decks/00000000-0000-0000-0000-0000000000ff", specToken, nil), 404)
}
