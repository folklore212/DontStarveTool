package main

import (
	"fmt"
	"log"
	"net/http"
	"time"
)

var startTime = time.Now().Unix()

func uptime() int64 {
	return time.Now().Unix() - startTime
}

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
}
