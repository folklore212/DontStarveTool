package main

import (
	"fmt"
	"os"
	"runtime"

	"github.com/folklore212/dst-platform/node/rpc"
)

type Executor struct {
	startTime int64
}

func NewExecutor() *Executor {
	return &Executor{}
}

func (e *Executor) Execute(method string, params interface{}) (interface{}, error) {
	switch method {
	case "node.health":
		return e.health(), nil
	case "node.metrics":
		return e.metrics(), nil
	default:
		return nil, fmt.Errorf("method %s not implemented on agent", method)
	}
}

func (e *Executor) health() *rpc.HealthResult {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	hostname, _ := os.Hostname()
	_ = hostname // reserved for future use

	return &rpc.HealthResult{
		Status:    "healthy",
		UptimeSec: uptime(),
		Version:   "0.1.0",
		GoVersion: runtime.Version(),
	}
}

func (e *Executor) metrics() *rpc.MetricsResult {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	r := &rpc.MetricsResult{}
	r.Memory.UsedGB = float64(m.Alloc) / 1024 / 1024 / 1024
	r.Memory.TotalGB = float64(m.Sys) / 1024 / 1024 / 1024
	return r
}
