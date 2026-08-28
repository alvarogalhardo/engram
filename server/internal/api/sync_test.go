package api

import (
	"fmt"
	"testing"
)

// Spec do milestone M2 — docs/sync-protocol.md §4–§6.
// Cada bloco de testes corresponde a uma issue (push / pull / reviews).

type pushResponse struct {
	Applied int      `json:"applied"`
	Stale   []string `json:"stale"`
}

type pullResponse struct {
	Decks   []deckJSON `json:"decks"`
	Cards   []cardJSON `json:"cards"`
	Next    int64      `json:"next"`
	HasMore bool       `json:"hasMore"`
}

func pushDeck(id string, name string, updatedAt int64, deleted bool) map[string]any {
	return map[string]any{
		"decks": []map[string]any{{
			"id": id, "name": name, "createdAt": updatedAt, "updatedAt": updatedAt, "deleted": deleted,
		}},
	}
}

const deckA = "11111111-1111-1111-1111-111111111111"

// --- issue M2: POST /v1/sync/push ---

func TestPushCreatesAndIsIdempotent(t *testing.T) {
	t.Skip("un-skip na issue M2: sync push (docs/roadmap.md)")
	s := newSpecServer(t)

	rec := doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 1000, false))
	wantStatus(t, rec, 200)
	if r := decode[pushResponse](t, rec); r.Applied != 1 {
		t.Fatalf("applied = %d, want 1", r.Applied)
	}

	// Retry idêntico (mesmo updatedAt): não pode duplicar nem falhar.
	rec = doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 1000, false))
	wantStatus(t, rec, 200)
	if r := decode[pushResponse](t, rec); r.Applied != 0 {
		t.Fatalf("retry aplicou de novo: %+v (LWW usa 'estritamente maior')", r)
	}
}

func TestPushLastWriteWins(t *testing.T) {
	t.Skip("un-skip na issue M2: sync push (docs/roadmap.md)")
	s := newSpecServer(t)

	doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "nova", 2000, false))

	// Escrita mais velha chega DEPOIS (device com relógio atrasado / sync tardio):
	// deve ser rejeitada e reportada em stale — é o caso de clock skew do LWW.
	rec := doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "velha", 1500, false))
	r := decode[pushResponse](t, rec)
	if r.Applied != 0 || len(r.Stale) != 1 || r.Stale[0] != deckA {
		t.Fatalf("escrita velha deveria ser stale: %+v", r)
	}
}

func TestPushPropagatesTombstone(t *testing.T) {
	t.Skip("un-skip na issue M2: sync push (docs/roadmap.md)")
	s := newSpecServer(t)
	doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 1000, false))
	doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 2000, true))
	wantStatus(t, doJSON(t, s, "GET", "/v1/decks/"+deckA, specToken, nil), 404)
}

// --- issue M2: GET /v1/sync/pull ---

func TestPullCursorAndPagination(t *testing.T) {
	t.Skip("un-skip na issue M2: sync pull (docs/roadmap.md)")
	s := newSpecServer(t)

	for i := 0; i < 3; i++ {
		id := fmt.Sprintf("22222222-2222-2222-2222-2222222222%02d", i)
		doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(id, fmt.Sprintf("d%d", i), int64(1000+i), false))
	}

	// Primeira página.
	rec := doJSON(t, s, "GET", "/v1/sync/pull?since=0&limit=2", specToken, nil)
	wantStatus(t, rec, 200)
	p1 := decode[pullResponse](t, rec)
	if len(p1.Decks) != 2 || !p1.HasMore {
		t.Fatalf("page1 = %+v, want 2 decks e hasMore", p1)
	}

	// Segunda página a partir do cursor.
	p2 := decode[pullResponse](t, doJSON(t, s, "GET",
		fmt.Sprintf("/v1/sync/pull?since=%d&limit=2", p1.Next), specToken, nil))
	if len(p2.Decks) != 1 || p2.HasMore {
		t.Fatalf("page2 = %+v, want 1 deck e fim", p2)
	}

	// Cursor no fim: pull vazio (nada novo).
	p3 := decode[pullResponse](t, doJSON(t, s, "GET",
		fmt.Sprintf("/v1/sync/pull?since=%d", p2.Next), specToken, nil))
	if len(p3.Decks) != 0 {
		t.Fatalf("pull no cursor final deveria ser vazio: %+v", p3)
	}
}

func TestPullIncludesTombstones(t *testing.T) {
	t.Skip("un-skip na issue M2: sync pull (docs/roadmap.md)")
	s := newSpecServer(t)
	doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 1000, false))
	doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 2000, true))

	p := decode[pullResponse](t, doJSON(t, s, "GET", "/v1/sync/pull?since=0", specToken, nil))
	if len(p.Decks) != 1 || !p.Decks[0].Deleted {
		t.Fatalf("pull deve entregar o tombstone para o outro device apagar: %+v", p)
	}
}

// --- issue M2: POST /v1/sync/reviews ---

func TestReviewUploadDeduplicates(t *testing.T) {
	t.Skip("un-skip na issue M2: review upload (docs/roadmap.md)")
	s := newSpecServer(t)
	doJSON(t, s, "POST", "/v1/sync/push", specToken, pushDeck(deckA, "Go", 1000, false))

	card := map[string]any{"cards": []map[string]any{{
		"id": "33333333-3333-3333-3333-333333333333", "deckId": deckA,
		"front": "f", "back": "b", "state": 0, "stepIndex": 0, "intervalDays": 0,
		"easeFactor": 2500, "repetitions": 0, "lapses": 0, "dueAt": 0,
		"createdAt": 1000, "updatedAt": 1000, "deleted": false,
	}}}
	doJSON(t, s, "POST", "/v1/sync/push", specToken, card)

	review := map[string]any{"reviews": []map[string]any{{
		"id":     "44444444-4444-4444-4444-444444444444",
		"cardId": "33333333-3333-3333-3333-333333333333", "deckId": deckA,
		"reviewedAt": 5000, "grade": 2, "stateBefore": 0,
		"intervalBeforeDays": 0, "intervalAfterDays": 1,
	}}}

	wantStatus(t, doJSON(t, s, "POST", "/v1/sync/reviews", specToken, review), 200)
	// Reenvio do mesmo evento (retry de rede): silenciosamente ignorado.
	wantStatus(t, doJSON(t, s, "POST", "/v1/sync/reviews", specToken, review), 200)
	// A contagem no banco deve ser 1 — verifique via query no seu teste se quiser
	// ir além; o contrato mínimo é o retry não falhar nem duplicar efeitos.
}
