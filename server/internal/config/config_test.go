package config

import "testing"

// Referência de table-driven test — o formato padrão de teste em Go.
func TestLoad(t *testing.T) {
	tests := []struct {
		name string
		env  map[string]string
		want Config
	}{
		{
			name: "defaults",
			env:  map[string]string{},
			want: Config{Port: "8080", DatabaseURL: "", APIToken: "dev-token-change-me"},
		},
		{
			name: "all set",
			env: map[string]string{
				"PORT":             "9090",
				"DATABASE_URL":     "postgres://x",
				"ENGRAM_API_TOKEN": "s3cret",
			},
			want: Config{Port: "9090", DatabaseURL: "postgres://x", APIToken: "s3cret"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := Load(func(k string) string { return tt.env[k] })
			if got != tt.want {
				t.Errorf("Load() = %+v, want %+v", got, tt.want)
			}
		})
	}
}
