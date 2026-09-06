package home

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"runtime"
	"time"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
	"github.com/AdguardTeam/AdGuardHome/internal/telebot"
)

var startTime = time.Now()

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
