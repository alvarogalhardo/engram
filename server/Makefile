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

# Migrations are NNNN_name.{up,down}.sql files under ./migrations, applied
# automatically when the server starts (store.Migrate).
# To add one: take the next number and write both the up and the down file.
