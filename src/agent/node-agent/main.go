package main

import (
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/folklore212/dst-platform/agent/shared"
)

func main() {
	configPath := flag.String("config", "/opt/dst-node/config.json", "path to config.json")
	flag.Parse()
	log.SetFlags(log.LstdFlags | log.Lshortfile)
	cfg, err := loadConfig(*configPath)
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}
	log.Printf("node-agent starting (platform: %s)", cfg.PlatformURL)
	agent := NewAgent(cfg)
	agent.Run()
	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	<-sigCh
	log.Println("shutting down...")
	agent.Stop()
	log.Println("node-agent stopped")
	_ = shared.Uptime // reference shared package
}
