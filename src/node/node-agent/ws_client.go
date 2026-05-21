package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/url"
	"sync"
	"time"

	"github.com/folklore212/dst-platform/node/rpc"
	"github.com/gorilla/websocket"
)

type Agent struct {
	config         *Config
	executor       *Executor
	conn           *websocket.Conn
	connMu         sync.Mutex
	reconnectDelay time.Duration
	requestID      int64
	done           chan struct{}
}

func NewAgent(cfg *Config) *Agent {
	return &Agent{
		config:         cfg,
		executor:       NewExecutor(cfg.DSTInstallPath, cfg.ClusterBasePath),
		reconnectDelay: 1 * time.Second,
		done:           make(chan struct{}),
	}
}

func (a *Agent) Connect() error {
	u, err := url.Parse(a.config.PlatformURL)
	if err != nil {
		return fmt.Errorf("invalid platform_url: %w", err)
	}
	q := u.Query()
	q.Set("token", a.config.Token)
	u.RawQuery = q.Encode()

	conn, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		return fmt.Errorf("dial failed: %w", err)
	}

	a.connMu.Lock()
	a.conn = conn
	a.connMu.Unlock()

	return nil
}

func (a *Agent) Run() {
	go a.connectLoop()
}

func (a *Agent) connectLoop() {
	for {
		select {
		case <-a.done:
			return
		default:
		}

		log.Printf("connecting to %s...", a.config.PlatformURL)
		if err := a.Connect(); err != nil {
			log.Printf("connection failed: %v (retry in %v)", err, a.reconnectDelay)
			time.Sleep(a.reconnectDelay)
			a.reconnectDelay = min(a.reconnectDelay*2, 60*time.Second)
			continue
		}

		log.Printf("connected to %s", a.config.PlatformURL)
		a.reconnectDelay = 1 * time.Second

		conn := a.getConn()
		if conn == nil {
			continue
		}

		heartbeatDone := make(chan struct{})
		go a.heartbeatLoop(conn, heartbeatDone)

		a.readLoop(conn)

		close(heartbeatDone)
		conn.Close()
		log.Printf("connection lost, reconnecting...")
	}
}

func (a *Agent) heartbeatLoop(conn *websocket.Conn, done chan struct{}) {
	interval := time.Duration(a.config.HeartbeatIntervalSec) * time.Second
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for {
		select {
		case <-done:
			return
		case <-ticker.C:
			a.connMu.Lock()
			err := conn.WriteMessage(websocket.PingMessage, nil)
			a.connMu.Unlock()
			if err != nil {
				return
			}
		}
	}
}

func (a *Agent) readLoop(conn *websocket.Conn) {
	pongWait := time.Duration(a.config.HeartbeatIntervalSec*2) * time.Second
	conn.SetReadDeadline(time.Now().Add(pongWait))
	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, msg, err := conn.ReadMessage()
		if err != nil {
			return
		}

		var req rpc.Request
		if err := json.Unmarshal(msg, &req); err != nil {
			continue
		}

		// Skip notifications (no method + no id)
		if req.Method == "" && req.ID == 0 {
			continue
		}

		go a.handleRequest(conn, &req)
	}
}

func (a *Agent) handleRequest(conn *websocket.Conn, req *rpc.Request) {
	result, err := a.executor.Execute(req.Method, req.Params)

	var resp rpc.Response
	if err != nil {
		resp = rpc.Response{
			JSONRPC: "2.0",
			Error:   &rpc.RPCError{Code: rpc.ErrInternal, Message: err.Error()},
			ID:      req.ID,
		}
	} else {
		resp = rpc.Response{
			JSONRPC: "2.0",
			Result:  result,
			ID:      req.ID,
		}
	}

	data, _ := json.Marshal(resp)
	a.connMu.Lock()
	defer a.connMu.Unlock()
	if a.conn == conn {
		conn.WriteMessage(websocket.TextMessage, data)
	}
}

func (a *Agent) getConn() *websocket.Conn {
	a.connMu.Lock()
	defer a.connMu.Unlock()
	return a.conn
}

func (a *Agent) Stop() {
	close(a.done)
	a.connMu.Lock()
	defer a.connMu.Unlock()
	if a.conn != nil {
		a.conn.Close()
	}
}
