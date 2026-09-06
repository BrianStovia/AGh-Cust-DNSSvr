package home

import (
	"net/http"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
	"github.com/AdguardTeam/AdGuardHome/internal/speedtest"
)

// handleGetSpeedtestRun handles GET /control/speedtest/run
func (web *webAPI) handleGetSpeedtestRun(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	res, err := speedtest.Run(ctx)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusTooManyRequests, "Speedtest: %s", err)
		return
	}

	l.InfoContext(ctx, "completed server speedtest benchmark",
		"download_mbps", res.DownloadMbps,
		"upload_mbps", res.UploadMbps,
		"ping_ms", res.PingMs,
		"jitter_ms", res.JitterMs,
		"isp", res.ISP,
	)

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, res)
}

// handleGetSpeedtestLast handles GET /control/speedtest/last
func (web *webAPI) handleGetSpeedtestLast(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	res := speedtest.GetLastResult()
	if res == nil {
		aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{
			"status":  "no_data",
			"message": "Belum ada riwayat pengujian speedtest",
		})
		return
	}

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, res)
}
