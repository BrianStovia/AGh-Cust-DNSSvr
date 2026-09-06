package home

import (
	"encoding/json"
	"net/http"
	"slices"
	"strings"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
	"github.com/AdguardTeam/AdGuardHome/internal/client"
	"github.com/AdguardTeam/AdGuardHome/internal/devicedetect"
	"github.com/AdguardTeam/golibs/logutil/slogutil"
)

// detectedDevicesResponse represents the response body for GET /control/devices/detected
type detectedDevicesResponse struct {
	TotalDevices int                       `json:"total_devices"`
	Summary      map[string]int            `json:"summary"` // device_type -> count
	Devices      []*devicedetect.DeviceInfo `json:"devices"`
}

// handleGetDetectedDevices handles GET /control/devices/detected
func (web *webAPI) handleGetDetectedDevices(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	detector := devicedetect.GetDetector()
	devices := detector.GetAllDevices()

	// Sort devices by last seen descending (most recent first)
	slices.SortFunc(devices, func(a, b *devicedetect.DeviceInfo) int {
		if a.LastSeen > b.LastSeen {
			return -1
		} else if a.LastSeen < b.LastSeen {
			return 1
		}
		return 0
	})

	summary := make(map[string]int)
	for _, d := range devices {
		summary[string(d.DeviceType)]++
	}

	resp := detectedDevicesResponse{
		TotalDevices: len(devices),
		Summary:      summary,
		Devices:      devices,
	}

	aghhttp.WriteJSONResponseOK(ctx, web.baseLogger, w, r, resp)
}

// handlePostClearDetectedDevices handles POST /control/devices/clear
func (web *webAPI) handlePostClearDetectedDevices(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.baseLogger.With(slogutil.KeyPrefix, "device_control")

	devicedetect.GetDetector().Clear()
	l.InfoContext(ctx, "cleared detected device discovery cache")

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{
		"ok":      true,
		"message": "Device discovery cache successfully cleared",
	})
}

// convertDeviceRequest represents request payload to convert a detected device to persistent client
type convertDeviceRequest struct {
	IP   string   `json:"ip"`
	Name string   `json:"name"`
	Tags []string `json:"tags"`
}

// handlePostConvertDevice handles POST /control/devices/convert
func (web *webAPI) handlePostConvertDevice(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.baseLogger.With(slogutil.KeyPrefix, "device_control")

	var req convertDeviceRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "decoding request: %s", err)
		return
	}

	req.IP = strings.TrimSpace(req.IP)
	if req.IP == "" {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "ip address is required")
		return
	}

	if req.Name == "" {
		req.Name = "Device " + req.IP
	}

	// Create persistent client
	p := &client.Persistent{
		Name: req.Name,
		Tags: req.Tags,
	}

	err = p.SetIDs([]string{req.IP})
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "setting client ip: %s", err)
		return
	}

	err = globalContext.clients.storage.Add(ctx, p)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "saving persistent client: %s", err)
		return
	}

	web.confModifier.Apply(ctx)

	l.InfoContext(ctx, "converted detected device to persistent client", "name", req.Name, "ip", req.IP)
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{
		"ok":      true,
		"message": "Device successfully saved as persistent client",
	})
}
