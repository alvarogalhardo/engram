// Package config loads the server configuration from environment variables.
// Load takes the lookup function as an argument so it can be tested without
// touching the real environment (cheap dependency injection, Go style).
package config

// Config is the full server configuration.
type Config struct {
	Port        string
	DatabaseURL string
	APIToken    string
}

// Load reads the configuration using the given lookup function (os.Getenv in production).
func Load(getenv func(string) string) Config {
	return Config{
		Port:        orDefault(getenv("PORT"), "8080"),
		DatabaseURL: getenv("DATABASE_URL"),
		APIToken:    orDefault(getenv("ENGRAM_API_TOKEN"), "dev-token-change-me"),
	}
}

func orDefault(v, def string) string {
	if v == "" {
		return def
	}
	return v
}
