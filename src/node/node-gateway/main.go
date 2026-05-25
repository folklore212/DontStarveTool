package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/folklore212/dst-platform/node/shared"
)

func main() {
	cfg := loadConfig()
	gw := newGateway(cfg)
	http.HandleFunc("/node", gw.HandleWebSocket)
	http.HandleFunc("/health", gw.HandleHealth)
	addr := fmt.Sprintf(":%s", cfg.Port)
	log.Printf("node-gateway listening on %s", addr)
	if err := http.ListenAndServe(addr, nil); err != nil {
		log.Fatalf("server failed: %v", err)
	}
	_ = shared.Uptime
}
