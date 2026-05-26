package main

import "testing"

func TestClusterNameValidation(t *testing.T) {
	tests := []struct {
		name  string
		input string
		valid bool
	}{
		{"valid simple", "my-cluster", true},
		{"valid underscore", "my_world_1", true},
		{"invalid semicolon", "test;rm -rf", false},
		{"invalid pipe", "test|cat", false},
		{"invalid space", "my world", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if reClusterName.MatchString(tt.input) != tt.valid {
				t.Errorf("clusterName=%q expected valid=%v", tt.input, tt.valid)
			}
		})
	}
}

func TestSteamIDValidation(t *testing.T) {
	tests := []struct {
		name  string
		input string
		valid bool
	}{
		{"valid", "76561198000000001", true},
		{"invalid short", "123", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if reSteamID.MatchString(tt.input) != tt.valid {
				t.Errorf("steamID=%q expected valid=%v", tt.input, tt.valid)
			}
		})
	}
}

func TestShardValidation(t *testing.T) {
	tests := []struct {
		name  string
		input string
		valid bool
	}{
		{"Master", "Master", true},
		{"Caves", "Caves", true},
		{"invalid", "x\"; rm -rf /", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if reShard.MatchString(tt.input) != tt.valid {
				t.Errorf("shard=%q expected valid=%v", tt.input, tt.valid)
			}
		})
	}
}

func TestStartParamsDefaultShards(t *testing.T) {
	p, err := unmarshalStartParams(map[string]interface{}{"cluster_name": "test-world"})
	if err != nil {
		t.Fatal(err)
	}
	if len(p.Shards) != 2 || p.Shards[0] != "Master" || p.Shards[1] != "Caves" {
		t.Errorf("got %v", p.Shards)
	}
}

func TestStartParamsInvalidClusterName(t *testing.T) {
	_, err := unmarshalStartParams(map[string]interface{}{"cluster_name": "test; rm -rf /"})
	if err == nil {
		t.Error("expected error")
	}
}

func TestConsoleSendEmptyCommand(t *testing.T) {
	_, err := unmarshalConsoleSendParams(map[string]interface{}{
		"cluster_name": "test-world", "shard": "Master", "command": "",
	})
	if err == nil {
		t.Error("expected error for empty command")
	}
}
