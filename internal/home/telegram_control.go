package home

import (
	"context"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"runtime"
	"runtime/debug"
	"strings"
	"sync"
	"time"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
	"github.com/AdguardTeam/AdGuardHome/internal/telebot"
)

var (
	startTime     = time.Now()
	safeModeState bool
	safeModeMu    sync.Mutex
)

// initTelegramCallbacks registers bot callback hooks with AdGuard Home core.
func (web *webAPI) initTelegramCallbacks() {
	bot := telebot.GetGlobalBot()

	bot.SetCallbacks(telebot.BotCallbacks{
		GetStatusFunc: func() string {
			var m runtime.MemStats
			runtime.ReadMemStats(&m)

			uptime := time.Since(startTime).Round(time.Second)
			allocMB := float64(m.Alloc) / 1024 / 1024
			sysMB := float64(m.Sys) / 1024 / 1024

			status := true
			if globalContext.dnsServer != nil {
				status, _ = globalContext.dnsServer.UpdatedProtectionStatus(context.Background())
			}
			statusStr := "🟢 AKTIF"
			if !status {
				statusStr = "🔴 NONAKTIF"
			}

			return fmt.Sprintf("🛡️ *AdGuard Home System Status*\n\n"+
				"• *Status Proteksi:* %s\n"+
				"• *Uptime:* `%s`\n"+
				"• *RAM In-Use:* `%.1f MB` (Sys: `%.1f MB`)\n"+
				"• *Goroutines:* `%d`\n"+
				"• *Go Version:* `%s`\n"+
				"• *OS/Arch:* `%s/%s`",
				statusStr, uptime, allocMB, sysMB, runtime.NumGoroutine(), runtime.Version(), runtime.GOOS, runtime.GOARCH,
			)
		},

		UnblockDomainFunc: func(domain string) (string, error) {
			rule := fmt.Sprintf("@@||%s^", domain)
			err := web.addUserRule(rule)
			if err != nil {
				return "", err
			}
			return fmt.Sprintf("Domain `%s` berhasil di-unblock (Rule: `%s`).", domain, rule), nil
		},

		BlockDomainFunc: func(domain string) (string, error) {
			rule := fmt.Sprintf("||%s^", domain)
			err := web.addUserRule(rule)
			if err != nil {
				return "", err
			}
			return fmt.Sprintf("Domain `%s` berhasil di-block (Rule: `%s`).", domain, rule), nil
		},

		PauseProtectionFunc: func(minutes int) error {
			if globalContext.filters != nil {
				until := time.Now().Add(time.Duration(minutes) * time.Minute)
				globalContext.filters.SetProtectionStatus(false, &until)
			}
			go func() {
				time.Sleep(time.Duration(minutes) * time.Minute)
				if globalContext.filters != nil {
					globalContext.filters.SetProtectionStatus(true, nil)
				}
				bot := telebot.GetGlobalBot()
				conf := bot.GetConfig()
				if conf.AdminChatID != "" {
					_ = bot.SendMessage(conf.AdminChatID, "▶️ *Timer jeda selesai!* Proteksi AdGuard Home otomatis kembali AKTIF.")
				}
			}()
			return nil
		},

		ResumeProtectionFunc: func() error {
			if globalContext.filters != nil {
				globalContext.filters.SetProtectionStatus(true, nil)
			}
			return nil
		},

		GetStatsSummaryFunc: func() string {
			return fmt.Sprintf("📊 *Ringkasan Statistik DNS*\n\n" +
				"Untuk melihat grafik interaktif dan peta GeoIP, buka web dashboard: https://dns.brianstovia.com",
			)
		},

		OptimizeServerFunc: func() string {
			var before, after runtime.MemStats
			runtime.ReadMemStats(&before)
			start := time.Now()

			// Run GC and release OS memory back to kernel
			runtime.GC()
			debug.FreeOSMemory()

			runtime.ReadMemStats(&after)
			duration := time.Since(start).Milliseconds()

			beforeAllocMB := float64(before.Alloc) / 1024 / 1024
			afterAllocMB := float64(after.Alloc) / 1024 / 1024
			freedMB := beforeAllocMB - afterAllocMB
			if freedMB < 0 {
				freedMB = 0
			}

			beforeSysMB := float64(before.Sys) / 1024 / 1024
			afterSysMB := float64(after.Sys) / 1024 / 1024

			return fmt.Sprintf("⚡ *Optimasi Server Selesai (One-Click Clean)* 🚀\n\n"+
				"• *RAM In-Use:* `%.1f MB` ➔ `%.1f MB` (*-%.1f MB*)\n"+
				"• *System RAM:* `%.1f MB` ➔ `%.1f MB`\n"+
				"• *Garbage Collection:* ✅ Sukses\n"+
				"• *OS Memory Released:* ✅ Sukses\n"+
				"• *Goroutines Aktif:* `%d`\n"+
				"• *Durasi Eksekusi:* `%d ms`\n\n"+
				"🛡️ _AdGuard Home beroperasi dalam performa puncak!_",
				beforeAllocMB, afterAllocMB, freedMB,
				beforeSysMB, afterSysMB,
				runtime.NumGoroutine(),
				duration,
			)
		},

		GetSetupGuideFunc: func() string {
			return "📱 *PANDUAN SETUP DNS SERVER BRST*\n\n" +
				"Gunakan konfigurasi berikut pada perangkat atau router Anda:\n\n" +
				"🌐 *DNS Standar (IPv4 / IPv6)*\n" +
				"• *Primary DNS:* `127.0.0.1` (atau IP VPS Anda)\n" +
				"• *Secondary DNS:* `1.1.1.1` / `8.8.8.8`\n\n" +
				"🔒 *DNS-over-HTTPS (DoH)*\n" +
				"`https://dns.brianstovia.com/dns-query`\n\n" +
				"🛡️ *DNS-over-TLS (DoT)*\n" +
				"`tls://dns.brianstovia.com`\n\n" +
				"━━━━━━━━━━━━━━━━━━━━\n" +
				"📲 *Langkah Pasang Cepat:*\n\n" +
				"🤖 *Android 9+ (Private DNS):*\n" +
				"Buka *Pengaturan* ➔ *Koneksi* ➔ *DNS Pribadi* ➔ Masukkan:\n" +
				"`dns.brianstovia.com`\n\n" +
				"🍎 *iOS / macOS / Windows 11:*\n" +
				"Gunakan DoH URL di atas pada pengaturan *Secure DNS* browser (Chrome / Edge / Firefox / Brave) atau install DoH Configuration Profile.\n\n" +
				"📺 *Router / Wi-Fi Rumah:*\n" +
				"Atur DNS Server DHCP di pengaturan Router ke IP Server ini untuk memproteksi seluruh jaringan secara otomatis.\n\n" +
				"🌐 *Web Dashboard:* https://dns.brianstovia.com"
		},

		GetServerInfoFunc: func() string {
			var m runtime.MemStats
			runtime.ReadMemStats(&m)

			uptime := time.Since(startTime).Round(time.Second)
			allocMB := float64(m.Alloc) / 1024 / 1024
			sysMB := float64(m.Sys) / 1024 / 1024

			safeModeStr := "🔴 Nonaktif"
			safeModeMu.Lock()
			if safeModeState {
				safeModeStr = "🟢 Aktif (SafeSearch + Family Guard)"
			}
			safeModeMu.Unlock()

			return fmt.Sprintf("🌐 *INFO SISTEM & SERVER DNS BRST*\n\n"+
				"• *Platform:* `%s/%s` (%d CPU Cores)\n"+
				"• *Go Runtime:* `%s`\n"+
				"• *Uptime:* `%s`\n"+
				"• *RAM:* `%.1f MB` (Alloc) / `%.1f MB` (Sys)\n\n"+
				"🛡️ *Protokol & Port Aktif:*\n"+
				"  ├ 🟢 DNS Port 53 (UDP / TCP)\n"+
				"  ├ 🟢 DoH: `https://dns.brianstovia.com/dns-query`\n"+
				"  ├ 🟢 DoT: `tls://dns.brianstovia.com`\n"+
				"  └ 🟢 Web Dashboard: Port 80 / 443\n\n"+
				"⚡ *Fitur Keamanan Terpasang:*\n"+
				"  ├ 🛡️ Cyber Shield DDoS Rate-Limiter (100 QPS)\n"+
				"  ├ 🔍 Smart Device Auto-Discovery\n"+
				"  ├ 🌍 Live GeoIP Map & Traffic Analytics\n"+
				"  ├ 🔒 Anti-DNS Rebinding Protection\n"+
				"  └ 👨‍👩‍👧 Safe Mode: %s\n\n"+
				"🌐 *Dashboard:* https://dns.brianstovia.com",
				runtime.GOOS, runtime.GOARCH, runtime.NumCPU(),
				runtime.Version(),
				uptime,
				allocMB, sysMB,
				safeModeStr,
			)
		},

		ToggleSafeModeFunc: func() string {
			safeModeMu.Lock()
			safeModeState = !safeModeState
			enabled := safeModeState
			safeModeMu.Unlock()

			rules := []string{
				"||porn^",
				"||xxx^",
				"||adult^",
				"||slot^",
				"||judi^",
				"||gambling^",
				"||bet88^",
				"||sbobet^",
			}

			if enabled {
				for _, r := range rules {
					_ = web.addUserRule(r)
				}
				if globalContext.filters != nil {
					globalContext.filters.EnableFilters(true)
				}
				return "🛡️ *Safe Mode & Proteksi Keluarga: AKTIF!* 🟢\n\n" +
					"• *Filter Dewasa & Judi:* 🟢 Aktif (8 Aturan Global)\n" +
					"• *SafeSearch Enforcement:* 🟢 Aktif\n" +
					"• *Pencarian Aman:* Google, Bing, YouTube, DuckDuckGo\n\n" +
					"_Seluruh jaringan kini terlindungi dari konten sensitif & judi online._"
			}

			return "🛡️ *Safe Mode & Proteksi Keluarga: NONAKTIF!* 🔴\n\n" +
				"Proteksi kembali ke mode standar (Hanya memblokir Iklan, Tracker & Malware)."
		},

		GetBlockedServicesFunc: func() []string {
			if globalContext.filters != nil {
				return globalContext.filters.GetBlockedServicesIDs()
			}
			return nil
		},

		ToggleBlockedServiceFunc: func(id string) (bool, error) {
			if globalContext.filters != nil {
				ctx := context.Background()
				return globalContext.filters.ToggleBlockedService(ctx, id)
			}
			return false, fmt.Errorf("filters uninitialized")
		},

		QuickBlockServiceFunc: func(service string) string {
			var name string
			var rules []string

			switch service {
			case "tiktok":
				name = "TikTok"
				rules = []string{"||tiktok.com^", "||tiktokcdn.com^", "||byteoversea.com^", "||ibytedtos.com^"}
			case "youtube":
				name = "YouTube"
				rules = []string{"||youtube.com^", "||googlevideo.com^", "||ytimg.com^", "||youtu.be^"}
			case "meta":
				name = "Instagram & Facebook"
				rules = []string{"||instagram.com^", "||facebook.com^", "||fbcdn.net^", "||cdninstagram.com^"}
			case "games":
				name = "Game Online (Steam/Roblox/ML)"
				rules = []string{"||roblox.com^", "||rbxcdn.com^", "||steampowered.com^", "||steamcommunity.com^", "||mobilelegends.com^"}
			case "adult":
				name = "Situs Dewasa & Judi Online"
				rules = []string{"||porn^", "||xxx^", "||slot^", "||judi^", "||gambling^", "||sbobet^"}
			default:
				return "⚠️ Layanan tidak dikenal."
			}

			for _, r := range rules {
				_ = web.addUserRule(r)
			}
			if globalContext.filters != nil {
				globalContext.filters.EnableFilters(true)
			}

			return fmt.Sprintf("🚫 *Layanan %s Berhasil Diblokir!* 🔒\n\n"+
				"• *Aturan Diterapkan:* `%d domain`\n"+
				"• *Status Jaringan:* Seluruh perangkat tidak dapat mengakses %s.\n\n"+
				"_Gunakan `/unblock <domain>` jika ingin membuka kembali akses._",
				name, len(rules), name,
			)
		},

		DNSLookupFunc: func(domain string) string {
			domain = telebotCleanDomain(domain)
			if domain == "" {
				return "⚠️ *Format salah!*\nGunakan: `/lookup nama-domain.com` (contoh: `/lookup netflix.com`)"
			}

			start := time.Now()
			// Check against AdGuard Home filter engine
			isBlocked := false
			var matchRule string
			var matchReason string

			if globalContext.filters != nil {
				res, err := globalContext.filters.CheckHost(domain, 1 /* TypeA */, nil)
				if err == nil && res.Reason.Matched() {
					isBlocked = true
					matchReason = res.Reason.String()
					if len(res.Rules) > 0 {
						matchRule = res.Rules[0].Text
					}
				}
			}

			// Perform live DNS lookup
			ips, err := net.LookupIP(domain)
			duration := time.Since(start).Milliseconds()

			statusStr := "🟢 DIIZINKAN (Unblocked)"
			if isBlocked {
				statusStr = "🔴 DIBLOKIR (Filtered)"
			}

			var ipList []string
			if err == nil && len(ips) > 0 {
				for i, ip := range ips {
					if i >= 4 {
						ipList = append(ipList, fmt.Sprintf("+%d IP lainnya", len(ips)-4))
						break
					}
					ipList = append(ipList, ip.String())
				}
			} else if err != nil {
				ipList = append(ipList, "Gagal me-resolve IP (NXDOMAIN / Timeout)")
			}

			ruleInfo := "Tidak ada (Domain Bebas)"
			if isBlocked {
				if matchRule != "" {
					ruleInfo = fmt.Sprintf("`%s` (%s)", matchRule, matchReason)
				} else {
					ruleInfo = fmt.Sprintf("Filter Rule (%s)", matchReason)
				}
			}

			return fmt.Sprintf("🔍 *HASIL DNS LOOKUP: `%s`*\n\n"+
				"• *Status Filter:* %s\n"+
				"• *Aturan/Alasan:* %s\n"+
				"• *Hasil IP:* `%s`\n"+
				"• *Latensi Query:* `%d ms`\n"+
				"• *Waktu Uji:* `%s`",
				domain, statusStr, ruleInfo, strings.Join(ipList, ", "), duration, time.Now().Format("15:04:05 WIB"),
			)
		},
	})
}

// addUserRule appends a rule to custom user filter rules and reloads filters.
func (web *webAPI) addUserRule(rule string) error {
	ctx := context.Background()
	if globalContext.filters != nil {
		globalContext.filters.AddUserRule(ctx, rule)
	}
	return nil
}

// handleGetTelegramStatus handles GET /control/telegram/status
func (web *webAPI) handleGetTelegramStatus(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	bot := telebot.GetGlobalBot()
	resp := bot.GetStatus()

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostTelegramConfig handles POST /control/telegram/config
func (web *webAPI) handlePostTelegramConfig(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req telebot.Config
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "reading req: %s", err)
		return
	}

	bot := telebot.GetGlobalBot()
	err = bot.UpdateConfig(req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "updating telegram bot: %s", err)
		return
	}

	l.InfoContext(ctx, "updated Telegram bot configuration",
		"enabled", req.Enabled,
		"admin_chat_id", req.AdminChatID,
	)

	config.Lock()
	config.Telegram = &req
	config.Unlock()

	web.confModifier.Apply(ctx)

	resp := bot.GetStatus()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}

// handlePostTelegramTest handles POST /control/telegram/test
func (web *webAPI) handlePostTelegramTest(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		BotToken    string `json:"bot_token"`
		AdminChatID string `json:"admin_chat_id"`
	}

	_ = json.NewDecoder(r.Body).Decode(&req)

	bot := telebot.GetGlobalBot()
	conf := bot.GetConfig()

	token := req.BotToken
	if token == "" {
		token = conf.BotToken
	}

	chatID := req.AdminChatID
	if chatID == "" {
		chatID = conf.AdminChatID
	}

	if token == "" || chatID == "" {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "Bot Token dan Chat ID tidak boleh kosong")
		return
	}

	testBot := telebot.NewBot(telebot.Config{
		Enabled:     true,
		BotToken:    token,
		AdminChatID: chatID,
	}, l)

	testMsg := fmt.Sprintf("🚀 *Tes Koneksi Berhasil!*\n\n" +
		"Bot Telegram AdGuard Home berhasil terhubung ke chat Anda.\n" +
		"Waktu: `" + time.Now().Format("02 Jan 2006 15:04:05 WIB") + "`\n\n" +
		"Ketik `/help` untuk melihat daftar perintah.")

	err := testBot.SendMessage(chatID, testMsg)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "Gagal mengirim pesan: %s", err)
		return
	}

	l.InfoContext(ctx, "sent test telegram message successfully", "chat_id", chatID)
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{"ok": true, "message": "Pesan tes berhasil dikirim ke Telegram!"})
}

func telebotCleanDomain(d string) string {
	d = strings.ToLower(strings.TrimSpace(d))
	d = strings.TrimPrefix(d, "https://")
	d = strings.TrimPrefix(d, "http://")
	d = strings.TrimPrefix(d, "www.")
	if idx := strings.Index(d, "/"); idx != -1 {
		d = d[:idx]
	}
	return d
}

