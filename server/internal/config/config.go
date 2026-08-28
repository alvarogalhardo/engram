// Package config carrega a configuração do servidor a partir de variáveis de
// ambiente. Load recebe a função de lookup para ser testável sem tocar no
// ambiente real (referência de injeção de dependência barata em Go).
package config

// Config é a configuração completa do servidor.
type Config struct {
	Port        string
	DatabaseURL string
	APIToken    string
}

// Load lê a configuração usando a função de lookup fornecida (os.Getenv em produção).
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
