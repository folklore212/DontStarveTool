package main

import (
	"os"
	"time"
)

type Config struct {
	Port              string
	CorePlatformURL   string
	ServerServiceURL  string
	TokenCacheTTL     time.Duration
}

func loadConfig() Config {
	cfg := Config{
		Port:             envOrDefault("PORT", "8090"),
		CorePlatformURL:  envOrDefault("CORE_PLATFORM_URL", "http://core-platform:8081"),
		ServerServiceURL: envOrDefault("SERVER_SERVICE_URL", "http://server-service:8083"),
		TokenCacheTTL:    60 * time.Second,
	}
	return cfg
}

func envOrDefault(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
