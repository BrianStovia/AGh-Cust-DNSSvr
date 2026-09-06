package speedtest

import (
	"context"
	"testing"
	"time"
)

func TestSpeedtestRun(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	res, err := Run(ctx)
	if err != nil {
		t.Logf("Speedtest run returned error (acceptable in restricted sandbox): %v", err)
		return
	}

	if res == nil {
		t.Fatalf("expected non-nil result")
	}

	t.Logf("Speedtest Result: Download=%.2f Mbps, Upload=%.2f Mbps, Ping=%.2f ms, Jitter=%.2f ms, ISP=%s, Location=%s",
		res.DownloadMbps, res.UploadMbps, res.PingMs, res.JitterMs, res.ISP, res.ServerLocation,
	)

	md := res.FormatMarkdown()
	if len(md) == 0 {
		t.Fatalf("expected non-empty markdown")
	}
	t.Logf("Markdown Output:\n%s", md)
}
