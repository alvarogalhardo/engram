// engram-server is the engram sync server: it exposes the HTTP API, applies
// migrations on startup, and boots in degraded mode when the database is down.
package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/alvarogalhardo/engram/server/internal/api"
	"github.com/alvarogalhardo/engram/server/internal/config"
	"github.com/alvarogalhardo/engram/server/internal/store"
)

func main() {
	if err := run(); err != nil {
		slog.Error("fatal", "err", err)
		os.Exit(1)
	}
}

func run() error {
	cfg := config.Load(os.Getenv)

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	// Without DATABASE_URL (or with the database down) the server boots degraded:
	// /healthz answers db=down and the data endpoints will return 503.
	var st *store.Store
	if cfg.DatabaseURL != "" {
		var err error
		st, err = store.Open(ctx, cfg.DatabaseURL)
		if err != nil {
			slog.Warn("database unavailable, starting degraded", "err", err)
			st = nil
		} else if err := st.Migrate(); err != nil {
			return err
		}
	} else {
		slog.Warn("DATABASE_URL not set, starting degraded")
	}

	srv := &http.Server{
		Addr:              ":" + cfg.Port,
		Handler:           api.New(cfg, st).Handler(),
		ReadHeaderTimeout: 5 * time.Second,
	}

	errCh := make(chan error, 1)
	go func() {
		slog.Info("listening", "addr", srv.Addr)
		errCh <- srv.ListenAndServe()
	}()

	select {
	case err := <-errCh:
		if !errors.Is(err, http.ErrServerClosed) {
			return err
		}
	case <-ctx.Done():
		slog.Info("shutting down")
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := srv.Shutdown(shutdownCtx); err != nil {
			return err
		}
	}
	if st != nil {
		st.Close()
	}
	return nil
}
