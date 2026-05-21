package main

import (
	"encoding/json"
	"os"
)

type Config struct {
	PlatformURL       string `json:"platform_url"`
	Token             string `json:"token"`
	DSTInstallPath    string `json:"dst_install_path"`
	ClusterBasePath   string `json:"cluster_base_path"`
	LogLevel          string `json:"log_level"`
	MetricsIntervalSec int   `json:"metrics_interval_sec"`
	HeartbeatIntervalSec int `json:"heartbeat_interval_sec"`
}

func loadConfig(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	cfg := &Config{
		DSTInstallPath:      "/home/steam/steamapps/DST",
		ClusterBasePath:     "/home/steam/.klei/DoNotStarveTogether",
		LogLevel:            "info",
		MetricsIntervalSec:  30,
		HeartbeatIntervalSec: 30,
	}
	if err := json.Unmarshal(data, cfg); err != nil {
		return nil, err
	}
	if cfg.MetricsIntervalSec <= 0 {
		cfg.MetricsIntervalSec = 30
	}
	if cfg.HeartbeatIntervalSec <= 0 {
		cfg.HeartbeatIntervalSec = 30
	}
	return cfg, nil
}
