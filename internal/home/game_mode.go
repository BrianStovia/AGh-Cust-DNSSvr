package home

import (
	"encoding/json"
	"net/http"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
	"github.com/AdguardTeam/AdGuardHome/internal/gamemode"
)

// handleGetGameModeStatus handles GET /control/gamemode/status
func (web *webAPI) handleGetGameModeStatus(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	resp := gamemode.GetStatus()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostGameModeToggle handles POST /control/gamemode/toggle
func (web *webAPI) handlePostGameModeToggle(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	newState := gamemode.Toggle()
	l.InfoContext(ctx, "toggled Smart Game Mode QoS", "enabled", newState)

	resp := gamemode.GetStatus()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostGameModeConfig handles POST /control/gamemode/config
func (web *webAPI) handlePostGameModeConfig(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		Enabled bool `json:"enabled"`
	}

	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "invalid request: %s", err)
		return
	}

	gamemode.SetEnabled(req.Enabled)
	l.InfoContext(ctx, "updated Smart Game Mode QoS config", "enabled", req.Enabled)

	resp := gamemode.GetStatus()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}
