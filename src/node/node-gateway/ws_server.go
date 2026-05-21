package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"

	"github.com/folklore212/dst-platform/node/rpc"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // nginx handles origin validation
	},
}

type gateway struct {
	config     Config
	tokens     *tokenCache
	disp       *dispatcher
	nodes      sync.Map // string(connID) → *nodeConn
	startTime  int64
}

type nodeConn struct {
	ID       string
	NodeInfo *TokenInfo
	Conn     *websocket.Conn
	SendCh   chan []byte
	CloseCh  chan struct{}
}

func newGateway(cfg Config) *gateway {
	return &gateway{
		config:    cfg,
		tokens:    newTokenCache(cfg.CorePlatformURL),
		disp:      newDispatcher(cfg.ServerServiceURL),
	}
}

func (g *gateway) HandleWebSocket(w http.ResponseWriter, r *http.Request) {
	token := r.URL.Query().Get("token")
	if token == "" {
		http.Error(w, "missing token", http.StatusUnauthorized)
		return
	}

	info, err := g.tokens.verify(token)
	if err != nil {
		log.Printf("auth failed: %v", err)
		http.Error(w, "invalid token", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("upgrade failed: %v", err)
		return
	}

	nc := &nodeConn{
		ID:       fmt.Sprintf("node-%d-%d", info.ServerID, info.NodeID),
		NodeInfo: info,
		Conn:     conn,
		SendCh:   make(chan []byte, 32),
		CloseCh:  make(chan struct{}),
	}

	g.nodes.Store(nc.ID, nc)
	log.Printf("node connected: %s (server=%d)", nc.ID, info.ServerID)

	go g.writeLoop(nc)
	go g.readLoop(nc)
}

func (g *gateway) readLoop(nc *nodeConn) {
	defer func() {
		close(nc.CloseCh)
		nc.Conn.Close()
		g.nodes.Delete(nc.ID)
		log.Printf("node disconnected: %s", nc.ID)
	}()

	for {
		_, msg, err := nc.Conn.ReadMessage()
		if err != nil {
			return
		}

		var req rpc.Request
		if err := json.Unmarshal(msg, &req); err != nil {
			resp := methodError(0, rpc.ErrParse, "invalid JSON")
			g.writeJSON(nc, resp)
			continue
		}

		resp := g.disp.dispatch(nc.NodeInfo.NodeID, nc.NodeInfo.ServerID, &req)
		g.writeJSON(nc, resp)
	}
}

func (g *gateway) writeLoop(nc *nodeConn) {
	for {
		select {
		case data := <-nc.SendCh:
			if err := nc.Conn.WriteMessage(websocket.TextMessage, data); err != nil {
				return
			}
		case <-nc.CloseCh:
			return
		}
	}
}

func (g *gateway) writeJSON(nc *nodeConn, resp *rpc.Response) {
	data, _ := json.Marshal(resp)
	select {
	case nc.SendCh <- data:
	default:
		// SendCh full — drop this message
		log.Printf("SendCh full for %s, dropping response", nc.ID)
	}
}

func (g *gateway) HandleHealth(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("ok"))
}
