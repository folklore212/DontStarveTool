package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/folklore212/dst-platform/node/rpc"
)

type dispatcher struct {
	serverServiceURL string
	httpClient       *http.Client
}

func newDispatcher(serverServiceURL string) *dispatcher {
	return &dispatcher{
		serverServiceURL: serverServiceURL,
		httpClient: &http.Client{
			Timeout: 30 * time.Second, // Note: using time import added below
		},
	}
}

func (d *dispatcher) dispatch(nodeID int64, serverID int64, req *rpc.Request) *rpc.Response {
	// node.health — answered locally by the gateway
	if req.Method == "node.health" {
		return handleHealth(req)
	}

	// All other methods — forward to server-service
	return d.forwardToServerService(nodeID, serverID, req)
}

func handleHealth(req *rpc.Request) *rpc.Response {
	return &rpc.Response{
		JSONRPC: "2.0",
		Result: rpc.HealthResult{
			Status:    "connected",
			UptimeSec: uptime(),
			Version:   "0.1.0",
			GoVersion: "go1.22",
		},
		ID: req.ID,
	}
}

type forwardRequest struct {
	NodeID   int64       `json:"nodeId"`
	ServerID int64       `json:"serverId"`
	Method   string      `json:"method"`
	Params   interface{} `json:"params"`
}

func (d *dispatcher) forwardToServerService(nodeID, serverID int64, req *rpc.Request) *rpc.Response {
	fr := forwardRequest{
		NodeID:   nodeID,
		ServerID: serverID,
		Method:   req.Method,
		Params:   req.Params,
	}
	body, err := json.Marshal(fr)
	if err != nil {
		return methodError(req.ID, rpc.ErrInternal, "marshal failed")
	}

	url := d.serverServiceURL + "/api/v1/internal/nodes/forward"
	httpResp, err := d.httpClient.Post(url, "application/json", bytes.NewReader(body))
	if err != nil {
		return methodError(req.ID, rpc.ErrInternal, fmt.Sprintf("forward failed: %v", err))
	}
	defer httpResp.Body.Close()

	var resp rpc.Response
	if err := json.NewDecoder(httpResp.Body).Decode(&resp); err != nil {
		return methodError(req.ID, rpc.ErrInternal, "invalid response from server")
	}
	return &resp
}

func methodError(id int64, code int, msg string) *rpc.Response {
	return &rpc.Response{
		JSONRPC: "2.0",
		Error:   &rpc.RPCError{Code: code, Message: msg},
		ID:      id,
	}
}

func methodNotFound(id int64) *rpc.Response {
	return methodError(id, rpc.ErrMethodNotFound, "method not found")
}
