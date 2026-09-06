package speedtest

import (
	"bytes"
	"context"
	"crypto/rand"
	"fmt"
	"io"
	"math"
	"net/http"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

// Result represents the speedtest benchmark metrics.
type Result struct {
	DownloadMbps   float64   `json:"download_mbps"`
	UploadMbps     float64   `json:"upload_mbps"`
	PingMs         float64   `json:"ping_ms"`
	JitterMs       float64   `json:"jitter_ms"`
	ISP            string    `json:"isp"`
	ServerLocation string    `json:"server_location"`
	ClientIP       string    `json:"client_ip"`
	Timestamp      time.Time `json:"timestamp"`
	DurationMs     int64     `json:"duration_ms"`
}

var (
	runningMu sync.Mutex
	isRunning bool
	lastResMu sync.RWMutex
	lastRes   *Result
)

// GetLastResult returns the most recent cached speedtest result.
func GetLastResult() *Result {
	lastResMu.RLock()
	defer lastResMu.RUnlock()
	return lastRes
}

// Run executes a comprehensive speedtest benchmark safely.
func Run(ctx context.Context) (*Result, error) {
	runningMu.Lock()
	if isRunning {
		runningMu.Unlock()
		return nil, fmt.Errorf("speedtest sedang berlangsung, silakan tunggu beberapa detik")
	}
	isRunning = true
	runningMu.Unlock()

	defer func() {
		runningMu.Lock()
		isRunning = false
		runningMu.Unlock()
	}()

	startTotal := time.Now()
	testCtx, cancel := context.WithTimeout(ctx, 16*time.Second)
	defer cancel()

	client := &http.Client{
		Timeout: 6 * time.Second,
		Transport: &http.Transport{
			MaxIdleConns:        10,
			IdleConnTimeout:     10 * time.Second,
			DisableCompression: true,
		},
	}

	// 1. Resolve ISP, IP & Location metadata
	clientIP, isp, loc := fetchMetadata(testCtx, client)

	// 2. Measure Ping & Jitter
	pingMs, jitterMs := measurePing(testCtx, client)

	// 3. Measure Download Speed (Multi-stream ~4s)
	downloadMbps := measureDownload(testCtx, client)

	// 4. Measure Upload Speed (Multi-stream ~3.5s)
	uploadMbps := measureUpload(testCtx, client)

	res := &Result{
		DownloadMbps:   math.Round(downloadMbps*10) / 10,
		UploadMbps:     math.Round(uploadMbps*10) / 10,
		PingMs:         math.Round(pingMs*10) / 10,
		JitterMs:       math.Round(jitterMs*10) / 10,
		ISP:            isp,
		ServerLocation: loc,
		ClientIP:       clientIP,
		Timestamp:      time.Now(),
		DurationMs:     time.Since(startTotal).Milliseconds(),
	}

	lastResMu.Lock()
	lastRes = res
	lastResMu.Unlock()

	return res, nil
}

func fetchMetadata(ctx context.Context, client *http.Client) (ip, isp, loc string) {
	ip = "Unknown"
	isp = "Cloudflare Anycast / Global CDN"
	loc = "Anycast Edge"

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, "https://1.1.1.1/cdn-cgi/trace", nil)
	if err != nil {
		return
	}

	resp, err := client.Do(req)
	if err != nil {
		return
	}
	defer func() { _ = resp.Body.Close() }()

	body, err := io.ReadAll(io.LimitReader(resp.Body, 4096))
	if err != nil {
		return
	}

	lines := strings.Split(string(body), "\n")
	for _, l := range lines {
		parts := strings.SplitN(l, "=", 2)
		if len(parts) == 2 {
			k := strings.TrimSpace(parts[0])
			v := strings.TrimSpace(parts[1])
			switch k {
			case "ip":
				ip = v
			case "colo":
				loc = fmt.Sprintf("Cloudflare Edge (%s)", v)
			case "loc":
				loc = fmt.Sprintf("%s (%s)", loc, v)
			}
		}
	}

	return
}

func measurePing(ctx context.Context, client *http.Client) (avgPing, jitter float64) {
	pingURL := "https://1.1.1.1/cdn-cgi/trace"
	var rtts []float64

	for i := 0; i < 5; i++ {
		select {
		case <-ctx.Done():
			break
		default:
		}

		t0 := time.Now()
		req, err := http.NewRequestWithContext(ctx, http.MethodHead, pingURL, nil)
		if err == nil {
			resp, errDo := client.Do(req)
			if errDo == nil {
				_ = resp.Body.Close()
				dur := float64(time.Since(t0).Microseconds()) / 1000.0
				rtts = append(rtts, dur)
			}
		}
		time.Sleep(50 * time.Millisecond)
	}

	if len(rtts) == 0 {
		return 0, 0
	}

	var sum float64
	for _, v := range rtts {
		sum += v
	}
	avgPing = sum / float64(len(rtts))

	if len(rtts) > 1 {
		var diffSum float64
		for i := 1; i < len(rtts); i++ {
			diffSum += math.Abs(rtts[i] - rtts[i-1])
		}
		jitter = diffSum / float64(len(rtts)-1)
	}

	return avgPing, jitter
}

func measureDownload(ctx context.Context, client *http.Client) float64 {
	dlCtx, cancel := context.WithTimeout(ctx, 4*time.Second)
	defer cancel()

	var totalBytes int64
	numWorkers := 3
	url := "https://speed.cloudflare.com/__down?bytes=25000000"

	var wg sync.WaitGroup
	startTime := time.Now()

	for i := 0; i < numWorkers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			buf := make([]byte, 32*1024)

			for {
				select {
				case <-dlCtx.Done():
					return
				default:
				}

				req, err := http.NewRequestWithContext(dlCtx, http.MethodGet, url, nil)
				if err != nil {
					return
				}

				resp, err := client.Do(req)
				if err != nil {
					return
				}

				for {
					n, rErr := resp.Body.Read(buf)
					if n > 0 {
						atomic.AddInt64(&totalBytes, int64(n))
					}
					if rErr != nil {
						break
					}
				}
				_ = resp.Body.Close()
			}
		}()
	}

	wg.Wait()
	elapsed := time.Since(startTime).Seconds()
	if elapsed <= 0.1 || totalBytes == 0 {
		return 0
	}

	// (Bytes * 8) / 1,000,000 / Seconds = Mbps
	return (float64(totalBytes) * 8.0) / (elapsed * 1000000.0)
}

func measureUpload(ctx context.Context, client *http.Client) float64 {
	upCtx, cancel := context.WithTimeout(ctx, 3500*time.Millisecond)
	defer cancel()

	var totalBytes int64
	numWorkers := 2
	url := "https://speed.cloudflare.com/__up"

	chunkSize := 1024 * 1024 // 1 MB payload buffer
	payload := make([]byte, chunkSize)
	_, _ = rand.Read(payload)

	var wg sync.WaitGroup
	startTime := time.Now()

	for i := 0; i < numWorkers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()

			for {
				select {
				case <-upCtx.Done():
					return
				default:
				}

				bodyReader := bytes.NewReader(payload)
				req, err := http.NewRequestWithContext(upCtx, http.MethodPost, url, bodyReader)
				if err != nil {
					return
				}
				req.Header.Set("Content-Type", "application/octet-stream")

				resp, err := client.Do(req)
				if err != nil {
					return
				}
				_, _ = io.Copy(io.Discard, resp.Body)
				_ = resp.Body.Close()

				atomic.AddInt64(&totalBytes, int64(chunkSize))
			}
		}()
	}

	wg.Wait()
	elapsed := time.Since(startTime).Seconds()
	if elapsed <= 0.1 || totalBytes == 0 {
		return 0
	}

	return (float64(totalBytes) * 8.0) / (elapsed * 1000000.0)
}

// FormatMarkdown returns a formatted Telegram card for the result.
func (r *Result) FormatMarkdown() string {
	downSpeed := fmt.Sprintf("%.1f Mbps", r.DownloadMbps)
	if r.DownloadMbps >= 1000 {
		downSpeed = fmt.Sprintf("%.2f Gbps", r.DownloadMbps/1000.0)
	}

	upSpeed := fmt.Sprintf("%.1f Mbps", r.UploadMbps)
	if r.UploadMbps >= 1000 {
		upSpeed = fmt.Sprintf("%.2f Gbps", r.UploadMbps/1000.0)
	}

	return fmt.Sprintf("🚀 *HASIL SPEEDTEST SERVER ADGUARD HOME* 📶\n\n"+
		"📥 *Download:* `%s`\n"+
		"📤 *Upload:* `%s`\n"+
		"⏱️ *Latency / Ping:* `%.1f ms` (Jitter: `%.1f ms`)\n\n"+
		"🌐 *IP Server:* `%s`\n"+
		"📍 *Edge Lokasi:* `%s`\n"+
		"🏢 *Jaringan / Backbone:* `%s`\n"+
		"⏰ *Waktu Pengujian:* `%s`\n"+
		"⏱️ *Durasi Tes:* `%.1f detik`\n\n"+
		"🛡️ _Koneksi server optimal untuk melayani resolusi DNS berkecepatan tinggi!_",
		downSpeed,
		upSpeed,
		r.PingMs, r.JitterMs,
		r.ClientIP,
		r.ServerLocation,
		r.ISP,
		r.Timestamp.Format("02 Jan 2006 15:04:05 WIB"),
		float64(r.DurationMs)/1000.0,
	)
}
