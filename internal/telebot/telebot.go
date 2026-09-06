package telebot

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

// Config represents the Telegram Bot configuration.
type Config struct {
	Enabled           bool   `json:"enabled"`
	BotToken          string `json:"bot_token"`
	AdminChatID       string `json:"admin_chat_id"`
	NotifyThreats     bool   `json:"notify_threats"`
	NotifyDDoS        bool   `json:"notify_ddos"`
	NotifyDailyReport bool   `json:"notify_daily_report"`
}

// BotCallbacks provides hooks into the AdGuard Home core engine.
type BotCallbacks struct {
	GetStatusFunc       func() string
	UnblockDomainFunc   func(domain string) (string, error)
	BlockDomainFunc     func(domain string) (string, error)
	PauseProtectionFunc func(minutes int) error
	ResumeProtectionFunc func() error
	GetStatsSummaryFunc func() string
}

// Status represents the runtime status of the bot.
type Status struct {
	Enabled       bool   `json:"enabled"`
	Connected     bool   `json:"connected"`
	BotUsername   string `json:"bot_username"`
	AdminChatID   string `json:"admin_chat_id"`
	NotifyThreats bool   `json:"notify_threats"`
	NotifyDDoS    bool   `json:"notify_ddos"`
	TotalAlertsSent uint64 `json:"total_alerts_sent"`
	LastAlertTime   string `json:"last_alert_time"`
}

// Bot manages the Telegram Bot long-polling and alert dispatching.
type Bot struct {
	mu          sync.RWMutex
	conf        Config
	callbacks   BotCallbacks
	httpClient  *http.Client
	ctx         context.Context
	cancel      context.CancelFunc
	logger      *slog.Logger
	running     bool
	connected   bool
	botUsername string
	alertsSent  uint64
	lastAlert   string
}

var (
	globalBot  *Bot
	globalOnce sync.Once
)

// GetGlobalBot returns the singleton Bot instance.
func GetGlobalBot() *Bot {
	globalOnce.Do(func() {
		globalBot = NewBot(Config{
			Enabled:           false,
			BotToken:          "",
			AdminChatID:       "",
			NotifyThreats:     true,
			NotifyDDoS:        true,
			NotifyDailyReport: true,
		}, slog.Default())
	})
	return globalBot
}

// NewBot creates a new Bot instance.
func NewBot(conf Config, logger *slog.Logger) *Bot {
	return &Bot{
		conf: conf,
		httpClient: &http.Client{
			Timeout: 35 * time.Second,
		},
		logger: logger.With("service", "telebot"),
	}
}

// SetCallbacks registers action callbacks into AdGuard Home core.
func (b *Bot) SetCallbacks(cb BotCallbacks) {
	b.mu.Lock()
	defer b.mu.Unlock()
	b.callbacks = cb
}

// Start initiates the Telegram Bot polling loop.
func (b *Bot) Start() error {
	b.mu.Lock()
	if b.running {
		b.mu.Unlock()
		return nil
	}

	if !b.conf.Enabled || strings.TrimSpace(b.conf.BotToken) == "" {
		b.mu.Unlock()
		return nil
	}

	b.ctx, b.cancel = context.WithCancel(context.Background())
	b.running = true
	b.mu.Unlock()

	// Verify bot token with getMe
	username, err := b.getMe()
	if err != nil {
		b.logger.Error("failed to connect to Telegram bot API", "err", err)
		b.mu.Lock()
		b.connected = false
		b.mu.Unlock()
		return err
	}

	b.mu.Lock()
	b.connected = true
	b.botUsername = username
	b.mu.Unlock()

	b.logger.Info("Telegram Bot connected successfully", "username", username)

	// Send startup notification if AdminChatID is set
	if b.conf.AdminChatID != "" {
		go func() {
			_ = b.SendMessage(b.conf.AdminChatID, fmt.Sprintf("🛡️ *AdGuard Home Cyber Shield Online!*\n\nBot @%s siap menerima perintah.\nKetik /help untuk panduan perintah.", username))
		}()
	}

	go b.pollLoop()
	return nil
}

// Stop gracefully shuts down the Telegram bot polling loop.
func (b *Bot) Stop() {
	b.mu.Lock()
	defer b.mu.Unlock()

	if !b.running {
		return
	}

	b.running = false
	b.connected = false
	if b.cancel != nil {
		b.cancel()
	}

	b.logger.Info("Telegram Bot stopped")
}

// UpdateConfig updates bot configuration and restarts if needed.
func (b *Bot) UpdateConfig(newConf Config) error {
	b.Stop()
	b.mu.Lock()
	b.conf = newConf
	b.mu.Unlock()

	if newConf.Enabled && strings.TrimSpace(newConf.BotToken) != "" {
		return b.Start()
	}
	return nil
}

// GetStatus returns the current status of the bot.
func (b *Bot) GetStatus() Status {
	b.mu.RLock()
	defer b.mu.RUnlock()

	return Status{
		Enabled:         b.conf.Enabled,
		Connected:       b.connected,
		BotUsername:     b.botUsername,
		AdminChatID:     b.conf.AdminChatID,
		NotifyThreats:   b.conf.NotifyThreats,
		NotifyDDoS:      b.conf.NotifyDDoS,
		TotalAlertsSent: atomic.LoadUint64(&b.alertsSent),
		LastAlertTime:   b.lastAlert,
	}
}

// GetConfig returns the current configuration.
func (b *Bot) GetConfig() Config {
	b.mu.RLock()
	defer b.mu.RUnlock()
	return b.conf
}

// SendMessage sends a text message to a specific Telegram chat.
func (b *Bot) SendMessage(chatID, text string) error {
	b.mu.RLock()
	token := b.conf.BotToken
	b.mu.RUnlock()

	if token == "" || chatID == "" {
		return errors.New("bot token or chat ID is empty")
	}

	apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/sendMessage", token)
	payload := map[string]any{
		"chat_id":    chatID,
		"text":       text,
		"parse_mode": "Markdown",
	}

	body, err := json.Marshal(payload)
	if err != nil {
		return err
	}

	req, err := http.NewRequestWithContext(context.Background(), http.MethodPost, apiURL, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := b.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer func() { _ = resp.Body.Close() }()

	if resp.StatusCode != http.StatusOK {
		respBody, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("telegram API error (status %d): %s", resp.StatusCode, string(respBody))
	}

	atomic.AddUint64(&b.alertsSent, 1)
	b.mu.Lock()
	b.lastAlert = time.Now().UTC().Format(time.RFC3339)
	b.mu.Unlock()

	return nil
}

// SendThreatAlert sends a high-priority notification when a threat domain is blocked.
func (b *Bot) SendThreatAlert(domain, clientIP, reason string) {
	b.mu.RLock()
	enabled := b.conf.Enabled && b.conf.NotifyThreats
	chatID := b.conf.AdminChatID
	b.mu.RUnlock()

	if !enabled || chatID == "" {
		return
	}

	msg := fmt.Sprintf("🚨 *ANCAMAN SIBER DIBLOKIR!*\n\n"+
		"🌐 *Domain:* `%s`\n"+
		"💻 *Klien:* `%s`\n"+
		"🛡️ *Alasan:* %s\n"+
		"⏰ *Waktu:* %s",
		domain, clientIP, reason, time.Now().Format("02 Jan 2006 15:04:05 WIB"),
	)

	go func() {
		_ = b.SendMessage(chatID, msg)
	}()
}

// SendDDoSAlert sends an alert when rate limiting is triggered.
func (b *Bot) SendDDoSAlert(clientIP string, qps int) {
	b.mu.RLock()
	enabled := b.conf.Enabled && b.conf.NotifyDDoS
	chatID := b.conf.AdminChatID
	b.mu.RUnlock()

	if !enabled || chatID == "" {
		return
	}

	msg := fmt.Sprintf("⚡ *RATE LIMIT / DDoS TRIGGERED!*\n\n"+
		"💻 *Client IP:* `%s`\n"+
		"📊 *Volume:* `%d query/detik`\n"+
		"🛡️ *Aksi:* Dibekukan sementara\n"+
		"⏰ *Waktu:* %s",
		clientIP, qps, time.Now().Format("02 Jan 2006 15:04:05 WIB"),
	)

	go func() {
		_ = b.SendMessage(chatID, msg)
	}()
}

func (b *Bot) getMe() (string, error) {
	apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/getMe", b.conf.BotToken)
	req, err := http.NewRequestWithContext(b.ctx, http.MethodGet, apiURL, nil)
	if err != nil {
		return "", err
	}

	resp, err := b.httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer func() { _ = resp.Body.Close() }()

	var result struct {
		OK     bool `json:"ok"`
		Result struct {
			Username  string `json:"username"`
			FirstName string `json:"first_name"`
		} `json:"result"`
		Description string `json:"description"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", err
	}

	if !result.OK {
		return "", fmt.Errorf("getMe error: %s", result.Description)
	}

	return result.Result.Username, nil
}

func (b *Bot) pollLoop() {
	var offset int64 = 0

	for {
		b.mu.RLock()
		running := b.running
		token := b.conf.BotToken
		b.mu.RUnlock()

		if !running {
			return
		}

		apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/getUpdates?timeout=25&offset=%d", token, offset)
		req, err := http.NewRequestWithContext(b.ctx, http.MethodGet, apiURL, nil)
		if err != nil {
			time.Sleep(2 * time.Second)
			continue
		}

		resp, err := b.httpClient.Do(req)
		if err != nil {
			if errors.Is(err, context.Canceled) {
				return
			}
			time.Sleep(3 * time.Second)
			continue
		}

		var updates struct {
			OK     bool `json:"ok"`
			Result []struct {
				UpdateID int64 `json:"update_id"`
				Message  *struct {
					MessageID int64 `json:"message_id"`
					From      struct {
						ID       int64  `json:"id"`
						Username string `json:"username"`
					} `json:"from"`
					Chat struct {
						ID   int64  `json:"id"`
						Type string `json:"type"`
					} `json:"chat"`
					Text string `json:"text"`
				} `json:"message"`
			} `json:"result"`
		}

		err = json.NewDecoder(resp.Body).Decode(&updates)
		_ = resp.Body.Close()

		if err != nil || !updates.OK {
			time.Sleep(2 * time.Second)
			continue
		}

		for _, update := range updates.Result {
			offset = update.UpdateID + 1
			if update.Message != nil && update.Message.Text != "" {
				go b.handleIncomingMessage(update.Message.Chat.ID, update.Message.Text)
			}
		}
	}
}

func (b *Bot) handleIncomingMessage(chatID int64, text string) {
	chatIDStr := strconv.FormatInt(chatID, 10)

	b.mu.RLock()
	adminChatID := b.conf.AdminChatID
	callbacks := b.callbacks
	b.mu.RUnlock()

	// If AdminChatID is set, only respond to admin
	if adminChatID != "" && adminChatID != chatIDStr {
		_ = b.SendMessage(chatIDStr, "⛔ *Akses Ditolak!*\nBot ini terkunci khusus untuk administrator.")
		return
	}

	trimmed := strings.TrimSpace(text)
	parts := strings.Fields(trimmed)
	if len(parts) == 0 {
		return
	}

	cmd := strings.ToLower(parts[0])
	// Strip bot mention e.g. /status@my_bot
	if idx := strings.Index(cmd, "@"); idx != -1 {
		cmd = cmd[:idx]
	}

	switch cmd {
	case "/start", "/help":
		msg := "🤖 *AdGuard Home Bot Controller*\n\n" +
			"Berikut perintah yang tersedia:\n" +
			"• `/status` — Lihat status server, RAM, & Uptime\n" +
			"• `/stats` — Ringkasan query & top domain\n" +
			"• `/unblock <domain>` — Buka blokir domain seketika\n" +
			"• `/block <domain>` — Masukkan domain ke blocklist\n" +
			"• `/pause [menit]` — Jeda filter sementara (default 10 menit)\n" +
			"• `/resume` — Nyalakan kembali filter adblock\n" +
			"• `/ping` — Uji responsivitas bot"
		_ = b.SendMessage(chatIDStr, msg)

	case "/ping":
		_ = b.SendMessage(chatIDStr, fmt.Sprintf("🏓 *Pong!*\nServer AdGuard Home aktif dan responsif.\nWaktu: `%s`", time.Now().Format("15:04:05 WIB")))

	case "/status":
		if callbacks.GetStatusFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetStatusFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "🟢 *Status:* Server Online & Proteksi Aktif.")
		}

	case "/stats":
		if callbacks.GetStatsSummaryFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetStatsSummaryFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "📊 *Statistik:* Statistik sedang dikumpulkan.")
		}

	case "/unblock":
		if len(parts) < 2 {
			_ = b.SendMessage(chatIDStr, "⚠️ *Format salah!*\nGunakan: `/unblock nama-domain.com`")
			return
		}
		target := cleanDomain(parts[1])
		if callbacks.UnblockDomainFunc != nil {
			res, err := callbacks.UnblockDomainFunc(target)
			if err != nil {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("❌ *Gagal unblock:* %s", err.Error()))
			} else {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("✅ *Sukses!*\n%s", res))
			}
		} else {
			_ = b.SendMessage(chatIDStr, fmt.Sprintf("✅ *Domain `%s` berhasil dibuka blokirnya.*", target))
		}

	case "/block":
		if len(parts) < 2 {
			_ = b.SendMessage(chatIDStr, "⚠️ *Format salah!*\nGunakan: `/block nama-domain.com`")
			return
		}
		target := cleanDomain(parts[1])
		if callbacks.BlockDomainFunc != nil {
			res, err := callbacks.BlockDomainFunc(target)
			if err != nil {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("❌ *Gagal block:* %s", err.Error()))
			} else {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("🛡️ *Sukses!*\n%s", res))
			}
		} else {
			_ = b.SendMessage(chatIDStr, fmt.Sprintf("🛡️ *Domain `%s` berhasil ditambahkan ke blocklist.*", target))
		}

	case "/pause":
		mins := 10
		if len(parts) >= 2 {
			if parsed, err := strconv.Atoi(parts[1]); err == nil && parsed > 0 {
				mins = parsed
			}
		}
		if callbacks.PauseProtectionFunc != nil {
			_ = callbacks.PauseProtectionFunc(mins)
		}
		_ = b.SendMessage(chatIDStr, fmt.Sprintf("⏸️ *Proteksi dijeda selama %d menit.*\nFilter akan otomatis menyala kembali setelah timer selesai, atau ketik `/resume`.", mins))

	case "/resume":
		if callbacks.ResumeProtectionFunc != nil {
			_ = callbacks.ResumeProtectionFunc()
		}
		_ = b.SendMessage(chatIDStr, "▶️ *Proteksi AdGuard Home kembali AKTIF!*")

	default:
		_ = b.SendMessage(chatIDStr, "❓ Perintah tidak dikenali. Ketik `/help` untuk melihat daftar perintah.")
	}
}

func cleanDomain(d string) string {
	d = strings.ToLower(strings.TrimSpace(d))
	d = strings.TrimPrefix(d, "https://")
	d = strings.TrimPrefix(d, "http://")
	d = strings.TrimPrefix(d, "www.")
	if idx := strings.Index(d, "/"); idx != -1 {
		d = d[:idx]
	}
	return d
}
