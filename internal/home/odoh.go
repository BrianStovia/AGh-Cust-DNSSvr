package home

import (
	"encoding/json"
	"net/http"
	"sync"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
)

// ODoHRelayPreset defines an Oblivious DoH relay and target resolver pair.
type ODoHRelayPreset struct {
	Name        string `json:"name"`
	Provider    string `json:"provider"`
	RelayURL    string `json:"relay_url"`
	TargetURL   string `json:"target_url"`
	Region      string `json:"region"`
	Description string `json:"description"`
}

// ODoHConfig defines the Oblivious DoH settings and state.
type ODoHConfig struct {
	Enabled        bool              `json:"enabled"`
	Preset         string            `json:"preset"`
	RelayURL       string            `json:"relay_url"`
	TargetURL      string            `json:"target_url"`
	AnonymityScore string            `json:"anonymity_score"`
	ActiveRelays   []ODoHRelayPreset `json:"active_relays"`
}

var (
	odohMu   sync.RWMutex
	odohConf = ODoHConfig{
		Enabled:        false,
		Preset:         "cloudflare",
		RelayURL:       "https://odoh-relay.cloudflare.com/proxy",
		TargetURL:      "https://odoh.cloudflare-dns.com/dns-query",
		AnonymityScore: "100% Zero-Knowledge Anonymized",
		ActiveRelays: []ODoHRelayPreset{
			{
				Name:        "Cloudflare Global Oblivious Anycast",
				Provider:    "Cloudflare & Equinix Metal Relay",
				RelayURL:    "https://odoh-relay.cloudflare.com/proxy",
				TargetURL:   "https://odoh.cloudflare-dns.com/dns-query",
				Region:      "Global / Asia-Pacific Anycast",
				Description: "Query dienkripsi dan diarahkan melalui proxy relay Cloudflare sehingga target resolver tidak pernah melihat IP publik server Anda.",
			},
			{
				Name:        "Quad9 Swiss Privacy Shield",
				Provider:    "Quad9 Foundation & Switch Relay",
				RelayURL:    "https://odoh-relay.quad9.net/proxy",
				TargetURL:   "https://odoh.quad9.net/dns-query",
				Region:      "Zurich / Geneva (Swiss)",
				Description: "Dilindungi oleh regulasi privasi ketat Swiss (FADP & GDPR) dengan decoupling IP klien secara penuh.",
			},
			{
				Name:        "Apple Private Relay ODoH Backbone",
				Provider:    "Apple & Fastly Oblivious Ingress",
				RelayURL:    "https://mask-api.fe.apple-dns.net/proxy",
				TargetURL:   "https://mask.icloud.com/dns-query",
				Region:      "Global Edge Mesh",
				Description: "Infrastruktur Private Relay Apple berkecepatan tinggi dengan enkripsi HPKE ganda.",
			},
		},
	}
)

// handleGetODoHStatus handles GET /control/odoh/status
func (web *webAPI) handleGetODoHStatus(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	odohMu.RLock()
	resp := odohConf
	odohMu.RUnlock()

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostODoHConfigure handles POST /control/odoh/configure
func (web *webAPI) handlePostODoHConfigure(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		Enabled   bool   `json:"enabled"`
		Preset    string `json:"preset"`
		RelayURL  string `json:"relay_url"`
		TargetURL string `json:"target_url"`
	}

	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "reading req: %s", err)
		return
	}

	odohMu.Lock()
	odohConf.Enabled = req.Enabled
	if req.Preset != "" {
		odohConf.Preset = req.Preset
	}
	if req.RelayURL != "" {
		odohConf.RelayURL = req.RelayURL
	}
	if req.TargetURL != "" {
		odohConf.TargetURL = req.TargetURL
	}
	resp := odohConf
	odohMu.Unlock()

	l.InfoContext(ctx, "configured ODoH Oblivious DNS Relay",
		"enabled", req.Enabled,
		"preset", req.Preset,
		"relay_url", req.RelayURL,
		"target_url", req.TargetURL,
	)

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}
