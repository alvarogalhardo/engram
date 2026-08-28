# Requires Go 1.25+ on PATH (see ../README.md).
GO ?= go

.PHONY: run test test-race lint build docker-build

run:
	$(GO) run ./cmd/engram-server

test:
	$(GO) test ./...

test-race:
	$(GO) test -race ./...

lint:
	golangci-lint run

build:
	CGO_ENABLED=0 $(GO) build -o bin/engram-server ./cmd/engram-server

docker-build:
	docker build -t engram-server .

# Migrations são arquivos NNNN_nome.{up,down}.sql em ./migrations,
# aplicadas automaticamente no startup do servidor (store.Migrate).
# Para criar uma nova: copie a numeração seguinte, escreva up e down.
