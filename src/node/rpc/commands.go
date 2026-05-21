package rpc

// ── Process management ──

type StartParams struct {
	ClusterName string   `json:"cluster_name"`
	Shards      []string `json:"shards"`
}

type StartResult struct {
	Processes map[string]ProcessInfo `json:"processes"`
}

type StopParams struct {
	ClusterName string `json:"cluster_name"`
	Force       bool   `json:"force"`
}

type StopResult struct {
	Stopped []string `json:"stopped"`
}

type RestartParams struct {
	ClusterName string `json:"cluster_name"`
}

type RestartResult struct{}

type StatusParams struct {
	ClusterName string `json:"cluster_name"`
}

type StatusResult struct {
	Shards map[string]ShardStatus `json:"shards"`
}

type ProcessInfo struct {
	PID   int    `json:"pid"`
	Screen string `json:"screen"`
}

type ShardStatus struct {
	Running     bool   `json:"running"`
	PID         int    `json:"pid"`
	UptimeSec   int    `json:"uptime_sec"`
	PlayerCount int    `json:"player_count"`
	Day         int    `json:"day"`
	Season      string `json:"season"`
}

// ── Console ──

type ConsoleSendParams struct {
	ClusterName string `json:"cluster_name"`
	Shard       string `json:"shard"`
	Command     string `json:"command"`
}

type ConsoleSendResult struct {
	Sent bool `json:"sent"`
}

// ── Players ──

type PlayersListParams struct {
	ClusterName string `json:"cluster_name"`
}

type PlayersListResult struct {
	Players []Player `json:"players"`
}

type PlayersKickParams struct {
	ClusterName string `json:"cluster_name"`
	Shard      string `json:"shard"`
	SteamID    string `json:"steam_id"`
	Reason     string `json:"reason"`
}

type PlayersKickResult struct{}

type PlayersBanParams struct {
	ClusterName string `json:"cluster_name"`
	SteamID    string `json:"steam_id"`
	Reason     string `json:"reason"`
}

type PlayersBanResult struct{}

type PlayersUnbanParams struct {
	ClusterName string `json:"cluster_name"`
	SteamID    string `json:"steam_id"`
}

type PlayersUnbanResult struct{}

type Player struct {
	Name       string `json:"name"`
	SteamID    string `json:"steam_id"`
	Character  string `json:"character"`
	PlayTimeMin int   `json:"play_time_min"`
	PingMs     int    `json:"ping_ms"`
}

// ── Admin list ──

type AdminListParams struct {
	ClusterName string `json:"cluster_name"`
}

type AdminListResult struct {
	Admins []string `json:"admins"`
}

// ── Node system ──

type HealthParams struct{}

type HealthResult struct {
	Status    string `json:"status"`
	UptimeSec int64  `json:"uptime_sec"`
	Version   string `json:"version"`
	GoVersion string `json:"go_version"`
}

type MetricsParams struct{}

type MetricsResult struct {
	CPU struct {
		Percent float64 `json:"percent"`
	} `json:"cpu"`
	Memory struct {
		UsedGB float64 `json:"used_gb"`
		TotalGB float64 `json:"total_gb"`
	} `json:"memory"`
	Disk struct {
		UsedGB    float64 `json:"used_gb"`
		TotalGB   float64 `json:"total_gb"`
		Percent   float64 `json:"percent"`
	} `json:"disk"`
	Network struct {
		RxMbps float64 `json:"rx_mbps"`
		TxMbps float64 `json:"tx_mbps"`
	} `json:"network"`
	DSTProcess struct {
		CPUPercent float64 `json:"cpu_percent"`
		MemMB      float64 `json:"mem_mb"`
	} `json:"dst_process"`
}
