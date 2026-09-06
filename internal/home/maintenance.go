package home

import (
	"context"
	"net/http"
	"runtime"
	"runtime/debug"
	"time"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
)

// maintenanceResultJSON is the response structure for maintenance optimization.
type maintenanceResultJSON struct {
	Status        string  `json:"status"`
	FreedMemoryMB float64 `json:"freed_memory_mb"`
	AllocatedMB   float64 `json:"allocated_mb"`
	SysMemoryMB   float64 `json:"sys_memory_mb"`
	NumGC         uint32  `json:"num_gc"`
	Timestamp     string  `json:"timestamp"`
}

// performMaintenance optimizes memory, flushes stale caches, and compacts runtime resources.
func performMaintenance(ctx context.Context) (res maintenanceResultJSON) {
	var mBefore runtime.MemStats
	runtime.ReadMemStats(&mBefore)

	// 1. Clear in-memory DNS cache if DNS server is running
	if globalContext.dnsServer != nil {
		globalContext.dnsServer.ClearCache()
	}

	// 2. Perform aggressive garbage collection and return memory to OS
	runtime.GC()
	debug.FreeOSMemory()

	var mAfter runtime.MemStats
	runtime.ReadMemStats(&mAfter)

	var freedMB float64
	if mBefore.Alloc > mAfter.Alloc {
		freedMB = float64(mBefore.Alloc-mAfter.Alloc) / (1024 * 1024)
	}

	return maintenanceResultJSON{
		Status:        "ok",
		FreedMemoryMB: float64(int(freedMB*100)) / 100,
		AllocatedMB:   float64(int(float64(mAfter.Alloc)/(1024*1024)*100)) / 100,
		SysMemoryMB:   float64(int(float64(mAfter.Sys)/(1024*1024)*100)) / 100,
		NumGC:         mAfter.NumGC,
		Timestamp:     time.Now().UTC().Format(time.RFC3339),
	}
}

// startMaintenanceScheduler runs periodic automatic maintenance every 5 minutes
// with an initial post-boot cleanup after 15 seconds to reclaim startup memory.
func (web *webAPI) startMaintenanceScheduler(ctx context.Context) {
	go func() {
		// Initial post-boot cleanup once filter lists and caches are initialized
		select {
		case <-ctx.Done():
			return
		case <-time.After(15 * time.Second):
			res := performMaintenance(ctx)
			web.logger.InfoContext(ctx, "initial post-boot memory cleanup completed",
				"freed_mb", res.FreedMemoryMB,
				"alloc_mb", res.AllocatedMB,
				"sys_mb", res.SysMemoryMB,
			)
		}

		ticker := time.NewTicker(5 * time.Minute)
		defer ticker.Stop()

		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				res := performMaintenance(ctx)
				if res.FreedMemoryMB > 5.0 {
					web.logger.DebugContext(ctx, "periodic auto-maintenance completed",
						"freed_mb", res.FreedMemoryMB,
						"alloc_mb", res.AllocatedMB,
						"sys_mb", res.SysMemoryMB,
					)
				}
			}
		}
	}()
}

// handlePostMaintenanceOptimize is the handler for POST /control/maintenance/optimize.
func (web *webAPI) handlePostMaintenanceOptimize(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	res := performMaintenance(ctx)
	l.InfoContext(ctx, "manual database & memory optimization performed",
		"freed_mb", res.FreedMemoryMB,
		"alloc_mb", res.AllocatedMB,
	)

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, res)
}
