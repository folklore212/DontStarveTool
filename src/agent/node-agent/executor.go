package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"time"
	"github.com/folklore212/dst-platform/agent/shared"
	"github.com/folklore212/dst-platform/agent/rpc"
)

var (
	reClusterName = regexp.MustCompile(`^[a-zA-Z0-9_-]{1,64}$`)
	reSteamID     = regexp.MustCompile(`^[0-9]{17}$`)
	reShard       = regexp.MustCompile(`^(Master|Caves)$`)
)

type Executor struct {
	dstPath     string
	clusterBase string
}

func NewExecutor(dstPath, clusterBase string) *Executor {
	return &Executor{
		dstPath:     dstPath,
		clusterBase: clusterBase,
	}
}

func (e *Executor) Execute(method string, params interface{}) (interface{}, error) {
	switch method {
	case "node.health":
		return e.health(), nil
	case "node.metrics":
		return e.metrics(), nil
	case "dst.start":
		return e.cmdStart(params)
	case "dst.stop":
		return e.cmdStop(params)
	case "dst.restart":
		return e.cmdRestart(params)
	case "dst.status":
		return e.cmdStatus(params)
	case "dst.console.send":
		return e.cmdConsoleSend(params)
	case "dst.players.list":
		return e.cmdPlayersList(params)
	case "dst.players.kick":
		return e.cmdPlayersKick(params)
	case "dst.players.ban":
		return e.cmdPlayersBan(params)
	case "dst.players.unban":
		return e.cmdPlayersUnban(params)
	case "dst.adminlist.get":
		return e.cmdAdminList(params)
	default:
		return nil, fmt.Errorf("method %s not implemented", method)
	}
}

func (e *Executor) health() *rpc.HealthResult {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)
	return &rpc.HealthResult{
		Status:    "healthy",
		UptimeSec: shared.Uptime(),
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

func (e *Executor) cmdStart(params interface{}) (*rpc.StartResult, error) {
	p, err := unmarshalStartParams(params)
	if err != nil {
		return nil, err
	}
	r := &rpc.StartResult{Processes: make(map[string]rpc.ProcessInfo)}
	dstBin := e.dstPath + "/bin/dontstarve_dedicated_server_nullrenderer"
	for _, shard := range p.Shards {
		sn := fmt.Sprintf("dst_%s_%s", p.ClusterName, shard)
		if err := exec.Command("screen", "-dmS", sn, dstBin,
			"-cluster", p.ClusterName, "-shard", shard).Run(); err != nil {
			return nil, fmt.Errorf("start %s: %w", shard, err)
		}
		time.Sleep(500 * time.Millisecond)
		r.Processes[shard] = rpc.ProcessInfo{PID: findScreenPID(sn), Screen: sn}
	}
	return r, nil
}

func (e *Executor) cmdStop(params interface{}) (*rpc.StopResult, error) {
	p, err := unmarshalStopParams(params)
	if err != nil {
		return nil, err
	}
	var stopped []string
	for _, shard := range []string{"Master", "Caves"} {
		sn := fmt.Sprintf("dst_%s_%s", p.ClusterName, shard)
		if exec.Command("screen", "-S", sn, "-X", "quit").Run() == nil {
			stopped = append(stopped, shard)
		}
	}
	if p.Force {
		exec.Command("pkill", "-f",
			"dontstarve_dedicated_server_nullrenderer.*"+p.ClusterName).Run()
	}
	return &rpc.StopResult{Stopped: stopped}, nil
}

func (e *Executor) cmdRestart(params interface{}) (*rpc.RestartResult, error) {
	e.cmdStop(params)
	time.Sleep(3 * time.Second)
	if _, err := e.cmdStart(params); err != nil {
		return nil, err
	}
	return &rpc.RestartResult{}, nil
}

func (e *Executor) cmdStatus(params interface{}) (*rpc.StatusResult, error) {
	p, err := unmarshalStatusParams(params)
	if err != nil {
		return nil, err
	}
	r := &rpc.StatusResult{Shards: make(map[string]rpc.ShardStatus)}
	for _, shard := range []string{"Master", "Caves"} {
		sn := fmt.Sprintf("dst_%s_%s", p.ClusterName, shard)
		pid := findScreenPID(sn)
		r.Shards[shard] = rpc.ShardStatus{Running: pid > 0, PID: pid}
	}
	return r, nil
}

func (e *Executor) cmdConsoleSend(params interface{}) (*rpc.ConsoleSendResult, error) {
	p, err := unmarshalConsoleSendParams(params)
	if err != nil {
		return nil, err
	}
	sn := fmt.Sprintf("dst_%s_%s", p.ClusterName, p.Shard)
	stuff := strings.ReplaceAll(p.Command, "\r", "") + "\r"
	if err := exec.Command("screen", "-S", sn, "-p", "0", "-X", "stuff", stuff).Run(); err != nil {
		return nil, fmt.Errorf("console send: %w", err)
	}
	return &rpc.ConsoleSendResult{Sent: true}, nil
}

func (e *Executor) cmdPlayersList(params interface{}) (*rpc.PlayersListResult, error) {
	p, err := unmarshalPlayersListParams(params)
	if err != nil {
		return nil, err
	}
	out, err := e.consoleRead(p.ClusterName, "Master",
		"for i, v in ipairs(TheNet:GetClientTable()) do print(string.format('%s|%s|%s', v.name or '', v.userid or '', v.prefab or '')) end")
	if err != nil {
		return nil, err
	}
	var players []rpc.Player
	sc := bufio.NewScanner(strings.NewReader(out))
	for sc.Scan() {
		p := strings.SplitN(sc.Text(), "|", 3)
		if len(p) >= 3 {
			players = append(players, rpc.Player{Name: p[0], SteamID: p[1], Character: p[2]})
		}
	}
	if players == nil {
		players = []rpc.Player{}
	}
	return &rpc.PlayersListResult{Players: players}, nil
}

func (e *Executor) cmdPlayersKick(params interface{}) (*rpc.PlayersKickResult, error) {
	p, err := unmarshalPlayersKickParams(params)
	if err != nil {
		return nil, err
	}
	if !reSteamID.MatchString(p.SteamID) {
		return nil, fmt.Errorf("invalid steam_id")
	}
	_, err = e.consoleRead(p.ClusterName, p.Shard,
		fmt.Sprintf(`TheNet:Kick("%s")`, p.SteamID))
	return &rpc.PlayersKickResult{}, err
}

func (e *Executor) cmdPlayersBan(params interface{}) (*rpc.PlayersBanResult, error) {
	p, err := unmarshalPlayersBanParams(params)
	if err != nil {
		return nil, err
	}
	if !reSteamID.MatchString(p.SteamID) {
		return nil, fmt.Errorf("invalid steam_id")
	}
	_, err = e.consoleRead(p.ClusterName, "Master",
		fmt.Sprintf(`TheNet:Ban("%s")`, p.SteamID))
	return &rpc.PlayersBanResult{}, err
}

func (e *Executor) cmdPlayersUnban(params interface{}) (*rpc.PlayersUnbanResult, error) {
	p, err := unmarshalPlayersUnbanParams(params)
	if err != nil {
		return nil, err
	}
	if !reSteamID.MatchString(p.SteamID) {
		return nil, fmt.Errorf("invalid steam_id")
	}
	_, err = e.consoleRead(p.ClusterName, "Master",
		fmt.Sprintf(`TheNet:UnBan("%s")`, p.SteamID))
	return &rpc.PlayersUnbanResult{}, err
}

func (e *Executor) cmdAdminList(params interface{}) (*rpc.AdminListResult, error) {
	p, err := unmarshalAdminListParams(params)
	if err != nil {
		return nil, err
	}
	out, err := e.consoleRead(p.ClusterName, "Master",
		"for i, v in ipairs(TheNet:GetAdminList()) do print(v) end")
	if err != nil {
		return nil, err
	}
	var admins []string
	sc := bufio.NewScanner(strings.NewReader(out))
	for sc.Scan() {
		if s := strings.TrimSpace(sc.Text()); s != "" {
			admins = append(admins, s)
		}
	}
	if admins == nil {
		admins = []string{}
	}
	return &rpc.AdminListResult{Admins: admins}, nil
}

func (e *Executor) consoleRead(cluster, shard, cmd string) (string, error) {
	sn := fmt.Sprintf("dst_%s_%s", cluster, shard)
	logFile := fmt.Sprintf("%s/%s/%s/server_log.txt", e.clusterBase, cluster, shard)
	info, err := os.Stat(logFile)
	seekPos := int64(0)
	if err == nil {
		seekPos = info.Size()
	}
	stuff := strings.ReplaceAll(cmd, "\r", "") + "\r"
	exec.Command("screen", "-S", sn, "-p", "0", "-X", "stuff", stuff).Run()
	time.Sleep(1500 * time.Millisecond)
	f, err := os.Open(logFile)
	if err != nil {
		return "", fmt.Errorf("cannot open log: %w", err)
	}
	defer f.Close()
	f.Seek(seekPos, 0)
	var lines []string
	sc := bufio.NewScanner(f)
	for sc.Scan() {
		lines = append(lines, sc.Text())
	}
	return strings.Join(lines, "\n"), nil
}

func findScreenPID(name string) int {
	out, err := exec.Command("screen", "-ls").Output()
	if err != nil {
		return 0
	}
	for _, line := range strings.Split(string(out), "\n") {
		if strings.Contains(line, name) {
			f := strings.Fields(line)
			if len(f) > 0 {
				pid, _ := strconv.Atoi(f[0])
				return pid
			}
		}
	}
	return 0
}

// ── Param unmarshal + validation ──

func mapParams(p interface{}) map[string]interface{} {
	m, _ := p.(map[string]interface{})
	return m
}

func strV(m map[string]interface{}, k string) string { s, _ := m[k].(string); return s }
func boolV(m map[string]interface{}, k string) bool { v, _ := m[k].(bool); return v }

func unmarshalStartParams(p interface{}) (*rpc.StartParams, error) {
	m := mapParams(p)
	r := &rpc.StartParams{ClusterName: strV(m, "cluster_name")}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	if arr, ok := m["shards"].([]interface{}); ok {
		for _, s := range arr {
			r.Shards = append(r.Shards, fmt.Sprint(s))
		}
	}
	if len(r.Shards) == 0 {
		r.Shards = []string{"Master", "Caves"}
	}
	return r, nil
}

func unmarshalStopParams(p interface{}) (*rpc.StopParams, error) {
	m := mapParams(p)
	r := &rpc.StopParams{ClusterName: strV(m, "cluster_name"), Force: boolV(m, "force")}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}

func unmarshalStatusParams(p interface{}) (*rpc.StatusParams, error) {
	m := mapParams(p)
	r := &rpc.StatusParams{ClusterName: strV(m, "cluster_name")}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}

func unmarshalConsoleSendParams(p interface{}) (*rpc.ConsoleSendParams, error) {
	m := mapParams(p)
	r := &rpc.ConsoleSendParams{
		ClusterName: strV(m, "cluster_name"),
		Shard:       strV(m, "shard"),
		Command:     strV(m, "command"),
	}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	if r.Command == "" {
		return nil, fmt.Errorf("command required")
	}
	return r, nil
}

func unmarshalPlayersListParams(p interface{}) (*rpc.PlayersListParams, error) {
	m := mapParams(p)
	r := &rpc.PlayersListParams{ClusterName: strV(m, "cluster_name")}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}

func unmarshalPlayersKickParams(p interface{}) (*rpc.PlayersKickParams, error) {
	m := mapParams(p)
	r := &rpc.PlayersKickParams{
		ClusterName: strV(m, "cluster_name"),
		Shard:       strV(m, "shard"),
		SteamID:     strV(m, "steam_id"),
		Reason:      strV(m, "reason"),
	}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}

func unmarshalPlayersBanParams(p interface{}) (*rpc.PlayersBanParams, error) {
	m := mapParams(p)
	r := &rpc.PlayersBanParams{
		ClusterName: strV(m, "cluster_name"),
		SteamID:     strV(m, "steam_id"),
		Reason:      strV(m, "reason"),
	}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}

func unmarshalPlayersUnbanParams(p interface{}) (*rpc.PlayersUnbanParams, error) {
	m := mapParams(p)
	r := &rpc.PlayersUnbanParams{
		ClusterName: strV(m, "cluster_name"),
		SteamID:     strV(m, "steam_id"),
	}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}

func unmarshalAdminListParams(p interface{}) (*rpc.AdminListParams, error) {
	m := mapParams(p)
	r := &rpc.AdminListParams{ClusterName: strV(m, "cluster_name")}
	if !reClusterName.MatchString(r.ClusterName) {
		return nil, fmt.Errorf("invalid cluster_name")
	}
	return r, nil
}
