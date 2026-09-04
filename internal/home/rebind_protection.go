package home

import (
	"encoding/json"
	"net/http"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
	"github.com/AdguardTeam/AdGuardHome/internal/dnsforward"
)

// handleGetRebindStatus handles GET /control/rebind/status
func (web *webAPI) handleGetRebindStatus(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	resp := dnsforward.GetRebindConfig()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostRebindConfig handles POST /control/rebind/config
func (web *webAPI) handlePostRebindConfig(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		Enabled            bool     `json:"enabled"`
		StrictMode         bool     `json:"strict_mode"`
		WhitelistedDomains []string `json:"whitelisted_domains"`
	}

	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "reading req: %s", err)
		return
	}

	resp := dnsforward.SetRebindConfig(req.Enabled, req.StrictMode, req.WhitelistedDomains)
	l.InfoContext(ctx, "configured Anti-DNS Rebinding Shield",
		"enabled", req.Enabled,
		"strict_mode", req.StrictMode,
		"whitelist_count", len(req.WhitelistedDomains),
	)

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostRebindClearStats handles POST /control/rebind/clear_stats
func (web *webAPI) handlePostRebindClearStats(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	dnsforward.ClearRebindStats()
	l.InfoContext(ctx, "cleared Anti-DNS Rebinding statistics")

	resp := dnsforward.GetRebindConfig()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}
