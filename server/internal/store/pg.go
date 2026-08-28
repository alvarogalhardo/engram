package store

import (
	"context"
	"database/sql"
	"errors"
	"time"

	"github.com/golang-migrate/migrate/v4"
	pgxmigrate "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	"github.com/golang-migrate/migrate/v4/source/iofs"
	"github.com/jackc/pgx/v5/pgxpool"
	_ "github.com/jackc/pgx/v5/stdlib" // database/sql "pgx" driver (used only by the migrations)

	"github.com/alvarogalhardo/engram/server/migrations"
)

// Store holds the Postgres connection pool.
type Store struct {
	Pool *pgxpool.Pool
	url  string
}

// Open connects to the database and validates the connection with a ping.
func Open(ctx context.Context, databaseURL string) (*Store, error) {
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, err
	}
	pingCtx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()
	if err := pool.Ping(pingCtx); err != nil {
		pool.Close()
		return nil, err
	}
	return &Store{Pool: pool, url: databaseURL}, nil
}

// Migrate applies the embedded migrations (server/migrations) on startup.
func (s *Store) Migrate() error {
	src, err := iofs.New(migrations.FS, ".")
	if err != nil {
		return err
	}
	db, err := sql.Open("pgx", s.url)
	if err != nil {
		return err
	}
	defer func() { _ = db.Close() }()
	driver, err := pgxmigrate.WithInstance(db, &pgxmigrate.Config{})
	if err != nil {
		return err
	}
	m, err := migrate.NewWithInstance("iofs", src, "pgx", driver)
	if err != nil {
		return err
	}
	if err := m.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return err
	}
	return nil
}

// Close releases the connection pool.
func (s *Store) Close() {
	s.Pool.Close()
}
