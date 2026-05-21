package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"
)

type TokenInfo struct {
	NodeID   int64  `json:"nodeId"`
	ServerID int64  `json:"serverId"`
	Valid    bool   `json:"valid"`
}

type verifyResponse struct {
	Code int        `json:"code"`
	Data *TokenInfo `json:"data"`
}

type tokenCache struct {
	mu         sync.RWMutex
	entries    map[string]*cacheEntry
	coreURL    string
	httpClient *http.Client
}

type cacheEntry struct {
	info    *TokenInfo
	expires time.Time
}

func newTokenCache(coreURL string) *tokenCache {
	return &tokenCache{
		entries: make(map[string]*cacheEntry),
		coreURL: coreURL,
		httpClient: &http.Client{
			Timeout: 5 * time.Second,
		},
	}
}

func (tc *tokenCache) verify(token string) (*TokenInfo, error) {
	// Check cache first
	tc.mu.RLock()
	if e, ok := tc.entries[token]; ok && time.Now().Before(e.expires) {
		tc.mu.RUnlock()
		return e.info, nil
	}
	tc.mu.RUnlock()

	// Call core-platform
	url := fmt.Sprintf("%s/api/v1/internal/nodes/verify?token=%s", tc.coreURL, token)
	resp, err := tc.httpClient.Get(url)
	if err != nil {
		return nil, fmt.Errorf("verify request failed: %w", err)
	}
	defer resp.Body.Close()

	var vr verifyResponse
	if err := json.NewDecoder(resp.Body).Decode(&vr); err != nil {
		return nil, fmt.Errorf("verify response parse: %w", err)
	}
	if vr.Code != 0 || vr.Data == nil {
		return nil, fmt.Errorf("invalid token")
	}
	if !vr.Data.Valid {
		return nil, fmt.Errorf("token disabled")
	}

	// Cache the result
	tc.mu.Lock()
	tc.entries[token] = &cacheEntry{
		info:    vr.Data,
		expires: time.Now().Add(60 * time.Second),
	}
	tc.mu.Unlock()

	return vr.Data, nil
}
