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
	Enabled           bool   `json:"enabled" yaml:"enabled"`
	BotToken          string `json:"bot_token" yaml:"bot_token"`
	AdminChatID       string `json:"admin_chat_id" yaml:"admin_chat_id"`
	NotifyThreats     bool   `json:"notify_threats" yaml:"notify_threats"`
	NotifyDDoS        bool   `json:"notify_ddos" yaml:"notify_ddos"`
	NotifyDailyReport bool   `json:"notify_daily_report" yaml:"notify_daily_report"`
}

// BotCallbacks provides hooks into the AdGuard Home core engine.
type BotCallbacks struct {
	GetStatusFunc               func() string
	UnblockDomainFunc           func(domain string) (string, error)
	BlockDomainFunc             func(domain string) (string, error)
	PauseProtectionFunc         func(minutes int) error
	ResumeProtectionFunc        func() error
	GetStatsSummaryFunc         func() string
	OptimizeServerFunc          func() string
	GetSetupGuideFunc           func() string
	GetServerInfoFunc           func() string
	ToggleSafeModeFunc          func() string
	QuickBlockServiceFunc       func(service string) string
	DNSLookupFunc               func(domain string) string
	GetBlockedServicesFunc      func() []string
	GetAllAvailableServicesFunc func() []BlockedServiceItem
	ToggleBlockedServiceFunc    func(id string) (bool, error)
	SetBlockedServiceFunc       func(id string, block bool) error
}

// Status represents the runtime status of the bot.
type Status struct {
	Enabled         bool   `json:"enabled"`
	Connected       bool   `json:"connected"`
	BotUsername     string `json:"bot_username"`
	AdminChatID     string `json:"admin_chat_id"`
	NotifyThreats   bool   `json:"notify_threats"`
	NotifyDDoS      bool   `json:"notify_ddos"`
	TotalAlertsSent uint64 `json:"total_alerts_sent"`
	LastAlertTime   string `json:"last_alert_time"`
}

// InlineKeyboardButton represents an interactive button in Telegram.
type InlineKeyboardButton struct {
	Text         string `json:"text"`
	CallbackData string `json:"callback_data"`
}

// InlineKeyboardMarkup represents rows of interactive buttons.
type InlineKeyboardMarkup struct {
	InlineKeyboard [][]InlineKeyboardButton `json:"inline_keyboard"`
}

// KeyboardButton represents a persistent menu button.
type KeyboardButton struct {
	Text string `json:"text"`
}

// ReplyKeyboardMarkup represents persistent bottom keyboard.
type ReplyKeyboardMarkup struct {
	Keyboard        [][]KeyboardButton `json:"keyboard"`
	ResizeKeyboard  bool               `json:"resize_keyboard"`
	OneTimeKeyboard bool               `json:"one_time_keyboard"`
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

	// Register command menu in Telegram UI
	_ = b.setMyCommands()

	// Send startup notification if AdminChatID is set
	if b.conf.AdminChatID != "" {
		go func() {
			_ = b.SendInteractiveMenu(b.conf.AdminChatID, fmt.Sprintf("🛡️ *AdGuard Home Cyber Shield Online!*\n\nBot @%s siap menerima perintah.\nGunakan menu tombol interaktif di bawah atau ketik perintah.", username))
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
	return b.sendMessageWithMarkup(chatID, text, nil)
}

// SendInteractiveMenu sends a message with inline buttons and reply keyboard.
func (b *Bot) SendInteractiveMenu(chatID, text string) error {
	inlineMarkup := &InlineKeyboardMarkup{
		InlineKeyboard: [][]InlineKeyboardButton{
			{
				{Text: "📊 Status Server", CallbackData: "cb:status"},
				{Text: "📈 Statistik Live", CallbackData: "cb:stats"},
			},
			{
				{Text: "⚡ Optimalkan Server", CallbackData: "cb:optimize"},
				{Text: "📱 Setup Guide", CallbackData: "cb:guide"},
			},
			{
				{Text: "🌐 Info Server & IP", CallbackData: "cb:netinfo"},
				{Text: "🛡️ Toggle Safe Mode", CallbackData: "cb:safemode"},
			},
			{
				{Text: "🚫 Blokir Layanan", CallbackData: "cb:services_menu"},
				{Text: "🔍 Cek Domain", CallbackData: "cb:lookup_info"},
			},
			{
				{Text: "⏸️ Jeda 10 Menit", CallbackData: "cb:pause_10"},
				{Text: "▶️ Resume Proteksi", CallbackData: "cb:resume"},
			},
			{
				{Text: "🏓 Ping Server", CallbackData: "cb:ping"},
				{Text: "📖 Panduan Command", CallbackData: "cb:help"},
			},
		},
	}

	return b.sendMessageWithMarkup(chatID, text, inlineMarkup)
}

// BlockedServiceItem defines a single service for Telegram pagination.
type BlockedServiceItem struct {
	ID      string `json:"id"`
	Name    string `json:"name"`
	GroupID string `json:"group_id"`
	Icon    string `json:"icon,omitempty"`
}

// getServiceIcon returns a representative emoji for a service.
func getServiceIcon(id, groupID string) string {
	switch id {
	case "youtube":
		return "▶️"
	case "netflix":
		return "🍿"
	case "spotify":
		return "🎵"
	case "disneyplus":
		return "✨"
	case "twitch":
		return "🟣"
	case "primevideo", "amazon_prime":
		return "📦"
	case "hulu":
		return "🟢"
	case "crunchyroll":
		return "🍣"
	case "bilibili":
		return "📺"
	case "tiktok":
		return "🎵"
	case "instagram":
		return "📸"
	case "facebook":
		return "📘"
	case "twitter", "x":
		return "🐦"
	case "reddit":
		return "🤖"
	case "threads":
		return "🧵"
	case "pinterest":
		return "📌"
	case "snapchat":
		return "👻"
	case "telegram":
		return "✈️"
	case "whatsapp":
		return "💬"
	case "discord":
		return "👾"
	case "skype":
		return "🗣️"
	case "steam":
		return "🎮"
	case "roblox":
		return "🧱"
	case "epic_games":
		return "🎯"
	case "riot_games", "valorant", "leagueoflegends":
		return "⚔️"
	case "blizzard", "activision_blizzard":
		return "❄️"
	case "pubg":
		return "🔫"
	case "minecraft":
		return "⛏️"
	case "genshin_impact":
		return "✨"
	case "playstation", "xbox":
		return "🎮"
	case "shopee":
		return "🛍️"
	case "tokopedia":
		return "📦"
	case "lazada":
		return "🛒"
	case "amazon":
		return "📦"
	case "ebay":
		return "🏷️"
	case "openai", "chatgpt":
		return "🧠"
	case "claude":
		return "🤖"
	case "pornhub", "onlyfans":
		return "🔞"
	case "tinder", "badoo":
		return "💜"
	case "9gag":
		return "🤣"
	case "4chan":
		return "🍀"
	}

	switch groupID {
	case "social_network":
		return "📱"
	case "streaming":
		return "🎬"
	case "gaming":
		return "🎮"
	case "messenger":
		return "💬"
	case "shopping":
		return "🛍️"
	case "dating":
		return "💜"
	case "gambling":
		return "🎰"
	case "ai":
		return "🤖"
	case "privacy":
		return "🔒"
	case "hosting":
		return "☁️"
	case "cdn":
		return "🌐"
	default:
		return "🌐"
	}
}

// DefaultPopularBlockedServices is the fallback catalog if dynamic engine list is unavailable.
var DefaultPopularBlockedServices = []BlockedServiceItem{
	{ID: "tiktok", Name: "TikTok", GroupID: "social_network", Icon: "📱"},
	{ID: "youtube", Name: "YouTube", GroupID: "streaming", Icon: "📺"},
	{ID: "instagram", Name: "Instagram", GroupID: "social_network", Icon: "📸"},
	{ID: "facebook", Name: "Facebook", GroupID: "social_network", Icon: "📘"},
	{ID: "twitter", Name: "Twitter / X", GroupID: "social_network", Icon: "🐦"},
	{ID: "telegram", Name: "Telegram", GroupID: "messenger", Icon: "💬"},
	{ID: "whatsapp", Name: "WhatsApp", GroupID: "messenger", Icon: "🟢"},
	{ID: "snapchat", Name: "Snapchat", GroupID: "social_network", Icon: "👻"},
	{ID: "netflix", Name: "Netflix", GroupID: "streaming", Icon: "🎬"},
	{ID: "spotify", Name: "Spotify", GroupID: "streaming", Icon: "🎵"},
	{ID: "disneyplus", Name: "Disney+", GroupID: "streaming", Icon: "🍿"},
	{ID: "twitch", Name: "Twitch", GroupID: "streaming", Icon: "🟣"},
	{ID: "discord", Name: "Discord", GroupID: "messenger", Icon: "👾"},
	{ID: "reddit", Name: "Reddit", GroupID: "social_network", Icon: "🤖"},
	{ID: "pinterest", Name: "Pinterest", GroupID: "social_network", Icon: "📌"},
	{ID: "threads", Name: "Threads", GroupID: "social_network", Icon: "🧵"},
	{ID: "roblox", Name: "Roblox", GroupID: "gaming", Icon: "🎮"},
	{ID: "steam", Name: "Steam", GroupID: "gaming", Icon: "🎮"},
	{ID: "epic_games", Name: "Epic Games", GroupID: "gaming", Icon: "🎮"},
	{ID: "riot_games", Name: "Riot Games", GroupID: "gaming", Icon: "🎮"},
	{ID: "blizzard", Name: "Battle.net", GroupID: "gaming", Icon: "🎮"},
	{ID: "pubg", Name: "PUBG", GroupID: "gaming", Icon: "🔫"},
	{ID: "9gag", Name: "9GAG", GroupID: "social_network", Icon: "🤣"},
	{ID: "4chan", Name: "4chan", GroupID: "social_network", Icon: "🍀"},
	{ID: "pornhub", Name: "Pornhub", GroupID: "dating", Icon: "🔞"},
	{ID: "onlyfans", Name: "OnlyFans", GroupID: "dating", Icon: "🔞"},
	{ID: "tinder", Name: "Tinder", GroupID: "dating", Icon: "🔥"},
	{ID: "badoo", Name: "Badoo", GroupID: "dating", Icon: "💜"},
	{ID: "shopee", Name: "Shopee", GroupID: "shopping", Icon: "🛍️"},
	{ID: "lazada", Name: "Lazada", GroupID: "shopping", Icon: "📦"},
	{ID: "amazon", Name: "Amazon", GroupID: "shopping", Icon: "📦"},
	{ID: "ebay", Name: "eBay", GroupID: "shopping", Icon: "🛒"},
}

// BuildBlockedServicesMenu builds the paginated keyboard and message for blocked services with category filters and search.
func (b *Bot) BuildBlockedServicesMenu(category string, page int, search string) (string, *InlineKeyboardMarkup) {
	b.mu.RLock()
	callbacks := b.callbacks
	b.mu.RUnlock()

	var allServices []BlockedServiceItem
	if callbacks.GetAllAvailableServicesFunc != nil {
		allServices = callbacks.GetAllAvailableServicesFunc()
	}
	if len(allServices) == 0 {
		allServices = DefaultPopularBlockedServices
	}

	var blockedMap = make(map[string]bool)
	if callbacks.GetBlockedServicesFunc != nil {
		for _, id := range callbacks.GetBlockedServicesFunc() {
			blockedMap[id] = true
		}
	}

	if category == "" {
		category = "all"
	}

	// Filter by Category and Search Query
	searchLower := strings.ToLower(strings.TrimSpace(search))
	var filtered []BlockedServiceItem

	for _, s := range allServices {
		// Category match
		if category != "all" {
			if category == "dating" {
				if s.GroupID != "dating" && s.GroupID != "gambling" {
					continue
				}
			} else if s.GroupID != category {
				continue
			}
		}

		// Search match
		if searchLower != "" {
			if !strings.Contains(strings.ToLower(s.ID), searchLower) && !strings.Contains(strings.ToLower(s.Name), searchLower) {
				continue
			}
		}

		icon := s.Icon
		if icon == "" {
			icon = getServiceIcon(s.ID, s.GroupID)
		}
		filtered = append(filtered, BlockedServiceItem{
			ID:      s.ID,
			Name:    s.Name,
			GroupID: s.GroupID,
			Icon:    icon,
		})
	}

	pageSize := 8
	totalFiltered := len(filtered)
	totalPages := (totalFiltered + pageSize - 1) / pageSize
	if totalPages < 1 {
		totalPages = 1
	}
	if page < 1 {
		page = 1
	}
	if page > totalPages {
		page = totalPages
	}

	startIdx := (page - 1) * pageSize
	endIdx := startIdx + pageSize
	if endIdx > totalFiltered {
		endIdx = totalFiltered
	}

	var currentItems []BlockedServiceItem
	if totalFiltered > 0 {
		currentItems = filtered[startIdx:endIdx]
	}

	var keyboard [][]InlineKeyboardButton

	// 1. Service Toggle Buttons (2 columns per row)
	var currentRow []InlineKeyboardButton
	for _, svc := range currentItems {
		isBlocked := blockedMap[svc.ID]
		btnLabel := fmt.Sprintf("🟢 %s %s", svc.Icon, svc.Name)
		if isBlocked {
			btnLabel = fmt.Sprintf("🔴 %s %s", svc.Icon, svc.Name)
		}

		currentRow = append(currentRow, InlineKeyboardButton{
			Text:         btnLabel,
			CallbackData: fmt.Sprintf("cb:bsvc_t:%s:%s:%d", svc.ID, category, page),
		})

		if len(currentRow) == 2 {
			keyboard = append(keyboard, currentRow)
			currentRow = []InlineKeyboardButton{}
		}
	}
	if len(currentRow) > 0 {
		keyboard = append(keyboard, currentRow)
	}

	// 2. Category Filter Tabs (4 columns x 2 rows)
	catRow1 := []InlineKeyboardButton{}
	catRow2 := []InlineKeyboardButton{}

	cats := []struct {
		id    string
		label string
	}{
		{"social_network", "📱 Sosmed"},
		{"streaming", "🎬 Stream"},
		{"gaming", "🎮 Game"},
		{"messenger", "💬 Chat"},
		{"shopping", "🛍️ Belanja"},
		{"dating", "🔞 Dewasa"},
		{"ai", "🤖 AI"},
		{"all", "📋 Semua"},
	}

	for i, c := range cats {
		lbl := c.label
		if category == c.id {
			lbl = "👉 " + c.label
		}
		btn := InlineKeyboardButton{
			Text:         lbl,
			CallbackData: fmt.Sprintf("cb:bsvc_c:%s", c.id),
		}
		if i < 4 {
			catRow1 = append(catRow1, btn)
		} else {
			catRow2 = append(catRow2, btn)
		}
	}
	keyboard = append(keyboard, catRow1, catRow2)

	// 3. Navigation Controls: [⬅️ Hal X] [📄 X/Y] [Hal Y ➡️]
	var navRow []InlineKeyboardButton
	if page > 1 {
		navRow = append(navRow, InlineKeyboardButton{
			Text:         fmt.Sprintf("⬅️ Hal %d", page-1),
			CallbackData: fmt.Sprintf("cb:bsvc_p:%s:%d", category, page-1),
		})
	}
	navRow = append(navRow, InlineKeyboardButton{
		Text:         fmt.Sprintf("📄 %d/%d (🔄)", page, totalPages),
		CallbackData: fmt.Sprintf("cb:bsvc_p:%s:%d", category, page),
	})
	if page < totalPages {
		navRow = append(navRow, InlineKeyboardButton{
			Text:         fmt.Sprintf("Hal %d ➡️", page+1),
			CallbackData: fmt.Sprintf("cb:bsvc_p:%s:%d", category, page+1),
		})
	}
	keyboard = append(keyboard, navRow)

	// 4. Bulk Block / Unblock this page
	keyboard = append(keyboard, []InlineKeyboardButton{
		{Text: "🔴 Blokir Halaman Ini", CallbackData: fmt.Sprintf("cb:bsvc_blk:block:%s:%d", category, page)},
		{Text: "🟢 Buka Halaman Ini", CallbackData: fmt.Sprintf("cb:bsvc_blk:unblock:%s:%d", category, page)},
	})

	// 5. Back to Main Menu
	keyboard = append(keyboard, []InlineKeyboardButton{
		{Text: "🔙 Kembali ke Menu Utama", CallbackData: "cb:menu"},
	})

	totalBlocked := len(blockedMap)
	catName := "Semua Layanan"
	for _, c := range cats {
		if c.id == category {
			catName = c.label
			break
		}
	}

	searchNote := ""
	if search != "" {
		searchNote = fmt.Sprintf("🔍 *Pencarian:* `%s`\n", search)
	}

	msg := fmt.Sprintf("🚫 *PUSAT KONTROL BLOCKED SERVICES*\n"+
		"Kontrol langsung toggle switch pemblokiran layanan di seluruh jaringan:\n\n"+
		"%s"+
		"📁 *Kategori:* *%s* (Halaman %d/%d)\n"+
		"• 🔴 = *Sedang DIBLOKIR* (Off di web)\n"+
		"• 🟢 = *DIIZINKAN* (On di web / Normal)\n"+
		"• *Total Terblokir Global:* `%d / %d Layanan`\n\n"+
		"_Ketuk tombol layanan untuk blokir/buka secara instan:_",
		searchNote, catName, page, totalPages, totalBlocked, len(allServices),
	)

	return msg, &InlineKeyboardMarkup{InlineKeyboard: keyboard}
}

// SendServicesMenu sends the interactive paginated blocked services menu.
func (b *Bot) SendServicesMenu(chatID string) error {
	msg, markup := b.BuildBlockedServicesMenu("all", 1, "")
	return b.sendMessageWithMarkup(chatID, msg, markup)
}

func (b *Bot) sendMessageWithMarkup(chatID, text string, replyMarkup any) error {
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

	if replyMarkup != nil {
		payload["reply_markup"] = replyMarkup
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

// EditMessageText updates an existing Telegram message in place.
func (b *Bot) EditMessageText(chatID int64, messageID int64, text string, replyMarkup any) error {
	b.mu.RLock()
	token := b.conf.BotToken
	b.mu.RUnlock()

	if token == "" || chatID == 0 || messageID == 0 {
		return errors.New("bot token, chat ID, or message ID is empty")
	}

	apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/editMessageText", token)
	payload := map[string]any{
		"chat_id":    chatID,
		"message_id": messageID,
		"text":       text,
		"parse_mode": "Markdown",
	}

	if replyMarkup != nil {
		payload["reply_markup"] = replyMarkup
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

	return nil
}

func (b *Bot) answerCallbackQuery(callbackID, text string) {
	b.mu.RLock()
	token := b.conf.BotToken
	b.mu.RUnlock()

	if token == "" || callbackID == "" {
		return
	}

	apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/answerCallbackQuery", token)
	payload := map[string]any{
		"callback_query_id": callbackID,
		"text":              text,
		"show_alert":        false,
	}

	body, _ := json.Marshal(payload)
	req, err := http.NewRequestWithContext(context.Background(), http.MethodPost, apiURL, bytes.NewReader(body))
	if err == nil {
		req.Header.Set("Content-Type", "application/json")
		resp, err := b.httpClient.Do(req)
		if err == nil {
			_ = resp.Body.Close()
		}
	}
}

// setMyCommands registers command list in Telegram UI.
func (b *Bot) setMyCommands() error {
	b.mu.RLock()
	token := b.conf.BotToken
	b.mu.RUnlock()

	if token == "" {
		return nil
	}

	apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/setMyCommands", token)
	payload := map[string]any{
		"commands": []map[string]string{
			{"command": "menu", "description": "📱 Buka Menu Tombol Interaktif"},
			{"command": "status", "description": "📊 Lihat Status Server & RAM"},
			{"command": "stats", "description": "📈 Ringkasan Statistik DNS"},
			{"command": "clean", "description": "⚡ Optimasi RAM & One-Click Clean"},
			{"command": "guide", "description": "📱 Setup Guide / Panduan Pasang DNS"},
			{"command": "netinfo", "description": "🌐 Info Jaringan, Port & IP Server"},
			{"command": "safemode", "description": "🛡️ Toggle SafeSearch & Mode Keluarga"},
			{"command": "services", "description": "🚫 Menu Blokir Layanan (TikTok, YouTube)"},
			{"command": "lookup", "description": "🔍 Cek Status Domain (/lookup domain.com)"},
			{"command": "unblock", "description": "✅ Buka Blokir Domain (/unblock nama.com)"},
			{"command": "block", "description": "🛡️ Masukkan Domain ke Blocklist (/block nama.com)"},
			{"command": "pause", "description": "⏸️ Jeda Filter 10 Menit (/pause 10)"},
			{"command": "resume", "description": "▶️ Aktifkan Kembali Proteksi"},
			{"command": "ping", "description": "🏓 Cek Kecepatan Respon Server"},
			{"command": "help", "description": "📖 Panduan Bantuan Lengkap"},
		},
	}

	body, _ := json.Marshal(payload)
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
				CallbackQuery *struct {
					ID   string `json:"id"`
					From struct {
						ID int64 `json:"id"`
					} `json:"from"`
					Message *struct {
						MessageID int64 `json:"message_id"`
						Chat      struct {
							ID int64 `json:"id"`
						} `json:"chat"`
					} `json:"message"`
					Data string `json:"data"`
				} `json:"callback_query"`
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

			// Handle regular text messages
			if update.Message != nil && update.Message.Text != "" {
				go b.handleIncomingMessage(update.Message.Chat.ID, update.Message.Text)
			}

			// Handle inline button clicks (Callback Queries)
			if update.CallbackQuery != nil && update.CallbackQuery.Data != "" {
				msgID := int64(0)
				chatObjID := int64(0)
				if update.CallbackQuery.Message != nil {
					msgID = update.CallbackQuery.Message.MessageID
					chatObjID = update.CallbackQuery.Message.Chat.ID
				}
				go b.handleCallbackQuery(update.CallbackQuery.ID, chatObjID, msgID, update.CallbackQuery.Data)
			}
		}
	}
}

func (b *Bot) handleCallbackQuery(callbackID string, chatID int64, messageID int64, data string) {
	chatIDStr := strconv.FormatInt(chatID, 10)

	b.mu.RLock()
	adminChatID := b.conf.AdminChatID
	callbacks := b.callbacks
	b.mu.RUnlock()

	if adminChatID != "" && adminChatID != chatIDStr {
		b.answerCallbackQuery(callbackID, "Akses Ditolak!")
		return
	}

	// Handle blocked services category change
	if strings.HasPrefix(data, "cb:bsvc_c:") {
		cat := strings.TrimPrefix(data, "cb:bsvc_c:")
		b.answerCallbackQuery(callbackID, "Kategori: "+cat)
		text, markup := b.BuildBlockedServicesMenu(cat, 1, "")
		if messageID > 0 {
			_ = b.EditMessageText(chatID, messageID, text, markup)
		} else {
			_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
		}
		return
	}

	// Handle blocked services page navigation & refresh
	if strings.HasPrefix(data, "cb:bsvc_p:") {
		parts := strings.Split(data, ":")
		cat := "all"
		page := 1
		if len(parts) >= 3 {
			cat = parts[2]
		}
		if len(parts) >= 4 {
			if p, err := strconv.Atoi(parts[3]); err == nil {
				page = p
			}
		}
		b.answerCallbackQuery(callbackID, fmt.Sprintf("Halaman %d", page))
		text, markup := b.BuildBlockedServicesMenu(cat, page, "")
		if messageID > 0 {
			_ = b.EditMessageText(chatID, messageID, text, markup)
		} else {
			_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
		}
		return
	}

	// Handle blocked services individual toggle
	if strings.HasPrefix(data, "cb:bsvc_t:") {
		parts := strings.Split(data, ":")
		if len(parts) >= 5 {
			svcID := parts[2]
			cat := parts[3]
			page := 1
			if p, err := strconv.Atoi(parts[4]); err == nil {
				page = p
			}

			if callbacks.ToggleBlockedServiceFunc != nil {
				blocked, err := callbacks.ToggleBlockedServiceFunc(svcID)
				if err != nil {
					b.answerCallbackQuery(callbackID, fmt.Sprintf("❌ Gagal: %s", err.Error()))
				} else if blocked {
					b.answerCallbackQuery(callbackID, fmt.Sprintf("🔴 %s DIBLOKIR!", svcID))
				} else {
					b.answerCallbackQuery(callbackID, fmt.Sprintf("🟢 %s DIIZINKAN!", svcID))
				}
			} else {
				b.answerCallbackQuery(callbackID, "⚠️ Handler belum siap.")
			}

			text, markup := b.BuildBlockedServicesMenu(cat, page, "")
			if messageID > 0 {
				_ = b.EditMessageText(chatID, messageID, text, markup)
			}
		}
		return
	}

	// Handle blocked services bulk page block / unblock
	if strings.HasPrefix(data, "cb:bsvc_blk:") {
		parts := strings.Split(data, ":")
		if len(parts) >= 5 {
			action := parts[2]
			cat := parts[3]
			page := 1
			if p, err := strconv.Atoi(parts[4]); err == nil {
				page = p
			}

			blockState := (action == "block")

			var allServices []BlockedServiceItem
			if callbacks.GetAllAvailableServicesFunc != nil {
				allServices = callbacks.GetAllAvailableServicesFunc()
			}
			if len(allServices) == 0 {
				allServices = DefaultPopularBlockedServices
			}

			var filtered []BlockedServiceItem
			for _, s := range allServices {
				if cat != "all" {
					if cat == "dating" {
						if s.GroupID != "dating" && s.GroupID != "gambling" {
							continue
						}
					} else if s.GroupID != cat {
						continue
					}
				}
				filtered = append(filtered, s)
			}

			pageSize := 8
			startIdx := (page - 1) * pageSize
			endIdx := startIdx + pageSize
			if endIdx > len(filtered) {
				endIdx = len(filtered)
			}

			if startIdx < len(filtered) && callbacks.SetBlockedServiceFunc != nil {
				for _, s := range filtered[startIdx:endIdx] {
					_ = callbacks.SetBlockedServiceFunc(s.ID, blockState)
				}
			}

			if blockState {
				b.answerCallbackQuery(callbackID, "🔴 Halaman ini DIBLOKIR!")
			} else {
				b.answerCallbackQuery(callbackID, "🟢 Halaman ini DIIZINKAN!")
			}

			text, markup := b.BuildBlockedServicesMenu(cat, page, "")
			if messageID > 0 {
				_ = b.EditMessageText(chatID, messageID, text, markup)
			}
		}
		return
	}

	switch data {
	case "cb:status":
		b.answerCallbackQuery(callbackID, "Memuat status server...")
		if callbacks.GetStatusFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetStatusFunc())
		}
	case "cb:stats":
		b.answerCallbackQuery(callbackID, "Memuat statistik DNS...")
		if callbacks.GetStatsSummaryFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetStatsSummaryFunc())
		}
	case "cb:pause_10":
		b.answerCallbackQuery(callbackID, "Proteksi dijeda 10 menit")
		if callbacks.PauseProtectionFunc != nil {
			_ = callbacks.PauseProtectionFunc(10)
		}
		_ = b.SendMessage(chatIDStr, "⏸️ *Proteksi dijeda selama 10 menit.*\nFilter otomatis aktif kembali setelah 10 menit.")
	case "cb:resume":
		b.answerCallbackQuery(callbackID, "Proteksi diaktifkan")
		if callbacks.ResumeProtectionFunc != nil {
			_ = callbacks.ResumeProtectionFunc()
		}
		_ = b.SendMessage(chatIDStr, "▶️ *Proteksi AdGuard Home kembali AKTIF!*")
	case "cb:ping":
		b.answerCallbackQuery(callbackID, "Pong! Server responsif")
		_ = b.SendMessage(chatIDStr, fmt.Sprintf("🏓 *Pong!*\nServer AdGuard Home aktif.\nWaktu: `%s`", time.Now().Format("15:04:05 WIB")))
	case "cb:optimize":
		b.answerCallbackQuery(callbackID, "⚡ Mengoptimalkan server...")
		if callbacks.OptimizeServerFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.OptimizeServerFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "⚡ Server telah dioptimalkan.")
		}
	case "cb:guide":
		b.answerCallbackQuery(callbackID, "📱 Membuka panduan setup...")
		if callbacks.GetSetupGuideFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetSetupGuideFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "📱 Buka https://dns.brianstovia.com untuk panduan setup.")
		}
	case "cb:netinfo":
		b.answerCallbackQuery(callbackID, "🌐 Memuat info jaringan & server...")
		if callbacks.GetServerInfoFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetServerInfoFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "🌐 Info server sedang dikumpulkan.")
		}
	case "cb:safemode":
		b.answerCallbackQuery(callbackID, "🛡️ Mengubah mode proteksi...")
		if callbacks.ToggleSafeModeFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.ToggleSafeModeFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "🛡️ Safe Mode diubah.")
		}
	case "cb:services_menu":
		b.answerCallbackQuery(callbackID, "🚫 Membuka Blocked Services...")
		text, markup := b.BuildBlockedServicesMenu("all", 1, "")
		if messageID > 0 {
			_ = b.EditMessageText(chatID, messageID, text, markup)
		} else {
			_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
		}
	case "cb:lookup_info":
		b.answerCallbackQuery(callbackID, "🔍 Cek Domain")
		msg := "🔍 *Fitur DNS Lookup & Cek Status Domain*\n\n" +
			"Ketik perintah:\n" +
			"`/lookup <nama-domain>`\n\n" +
			"_Contoh:_\n" +
			"• `/lookup netflix.com`\n" +
			"• `/lookup tiktok.com`\n" +
			"• `/lookup google.com`\n\n" +
			"Bot akan memeriksa apakah domain tersebut diizinkan/diblokir dan mengecek waktu respon resolusi DNS-nya."
		_ = b.SendMessage(chatIDStr, msg)
	case "cb:menu":
		b.answerCallbackQuery(callbackID, "Membuka Menu")
		_ = b.SendInteractiveMenu(chatIDStr, "🤖 *Panel Kontrol Interaktif AdGuard Home*\n\nPilih aksi di bawah ini:")
	case "cb:help":
		b.answerCallbackQuery(callbackID, "Membuka panduan")
		msg := "📖 *Panduan Perintah AdGuard Home Bot*\n\n" +
			"• `/menu` — Tampilkan menu tombol interaktif\n" +
			"• `/status` — Status server, RAM, Uptime & Klien\n" +
			"• `/stats` — Statistik query & top domain\n" +
			"• `/clean` — ⚡ Optimasi RAM & One-Click Clean\n" +
			"• `/guide` — 📱 Setup Guide & Panduan Pasang DNS\n" +
			"• `/netinfo` — 🌐 Info Jaringan, Port & Protokol\n" +
			"• `/safemode` — 🛡️ Toggle SafeSearch & Mode Keluarga\n" +
			"• `/services` — 🚫 Menu & Toggle Blocked Services (TikTok, YouTube, Games, dll.)\n" +
			"• `/lookup <domain>` — 🔍 Cek Status Domain\n" +
			"• `/unblock <target>` — Buka blokir domain / layanan (contoh: `/unblock tiktok` atau `/unblock reddit.com`)\n" +
			"• `/block <target>` — Blokir domain / layanan (contoh: `/block youtube` atau `/block slot.com`)\n" +
			"• `/pause [menit]` — Jeda filter sementara (contoh: `/pause 15`)\n" +
			"• `/resume` — Aktifkan kembali filter\n" +
			"• `/ping` — Uji kecepatan respon server"
		_ = b.SendMessage(chatIDStr, msg)
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
	case "/start", "/menu", "menu", "📋 menu":
		msg := "🤖 *Panel Kontrol Interaktif AdGuard Home*\n\n" +
			"Pilih menu aksi cepat di bawah atau ketik perintah langsung:"
		_ = b.SendInteractiveMenu(chatIDStr, msg)

	case "/help", "bantuan", "help":
		msg := "📖 *Panduan Perintah AdGuard Home Bot*\n\n" +
			"• `/menu` — Buka menu tombol interaktif\n" +
			"• `/status` — Lihat status server, RAM, & Uptime\n" +
			"• `/stats` — Ringkasan query & top domain\n" +
			"• `/clean` — ⚡ Optimasi RAM & One-Click Clean\n" +
			"• `/guide` — 📱 Setup Guide & Panduan Pasang DNS\n" +
			"• `/netinfo` — 🌐 Info Jaringan, Port & Protokol\n" +
			"• `/safemode` — 🛡️ Toggle SafeSearch & Mode Keluarga\n" +
			"• `/services` — 🚫 Menu & Toggle Blocked Services\n" +
			"• `/lookup <domain>` — 🔍 Cek Status Domain\n" +
			"• `/unblock <domain/layanan>` — Buka blokir domain/layanan seketika\n" +
			"• `/block <domain/layanan>` — Masukkan domain/layanan ke blocklist\n" +
			"• `/pause [menit]` — Jeda filter sementara (default 10 menit)\n" +
			"• `/resume` — Nyalakan kembali filter adblock\n" +
			"• `/ping` — Uji responsivitas bot"
		_ = b.SendMessage(chatIDStr, msg)

	case "/netinfo", "/server", "/ip", "🌐 info server & ip":
		if callbacks.GetServerInfoFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetServerInfoFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "🌐 Info server sedang dikumpulkan.")
		}

	case "/safemode", "/family", "🛡️ toggle safe mode":
		if callbacks.ToggleSafeModeFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.ToggleSafeModeFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "🛡️ Safe Mode diubah.")
		}

	case "/services", "/blockservice", "/blocked", "/bsvc", "🚫 blokir layanan":
		if len(parts) > 1 {
			arg := strings.ToLower(parts[1])
			switch arg {
			case "sosmed", "social", "social_network":
				text, markup := b.BuildBlockedServicesMenu("social_network", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			case "stream", "streaming", "video", "musik", "music":
				text, markup := b.BuildBlockedServicesMenu("streaming", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			case "game", "gaming", "games":
				text, markup := b.BuildBlockedServicesMenu("gaming", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			case "chat", "messenger", "pesan":
				text, markup := b.BuildBlockedServicesMenu("messenger", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			case "belanja", "shop", "shopping":
				text, markup := b.BuildBlockedServicesMenu("shopping", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			case "dewasa", "dating", "judi", "gambling", "adult":
				text, markup := b.BuildBlockedServicesMenu("dating", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			case "ai", "llm":
				text, markup := b.BuildBlockedServicesMenu("ai", 1, "")
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			default:
				text, markup := b.BuildBlockedServicesMenu("all", 1, arg)
				_ = b.sendMessageWithMarkup(chatIDStr, text, markup)
			}
			return
		}
		_ = b.SendServicesMenu(chatIDStr)

	case "/lookup", "/check", "/cek", "🔍 cek domain":
		if len(parts) < 2 {
			_ = b.SendMessage(chatIDStr, "⚠️ *Format salah!*\nGunakan: `/lookup nama-domain.com` (contoh: `/lookup netflix.com`)")
			return
		}
		if callbacks.DNSLookupFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.DNSLookupFunc(parts[1]))
		} else {
			_ = b.SendMessage(chatIDStr, fmt.Sprintf("🔍 Memeriksa domain `%s`...", parts[1]))
		}

	case "/clean", "/optimize", "clean", "optimize", "⚡ optimalkan server", "⚡ optimalkan sekarang", "bersihkan":
		if callbacks.OptimizeServerFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.OptimizeServerFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "⚡ Server telah dioptimalkan.")
		}

	case "/guide", "/setup", "guide", "setup", "📱 panduan setup", "panduan", "setup guide":
		if callbacks.GetSetupGuideFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetSetupGuideFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "📱 Kunjungi https://dns.brianstovia.com untuk panduan setup.")
		}

	case "/ping", "ping", "🏓 ping":
		_ = b.SendMessage(chatIDStr, fmt.Sprintf("🏓 *Pong!*\nServer AdGuard Home aktif dan responsif.\nWaktu: `%s`", time.Now().Format("15:04:05 WIB")))

	case "/status", "status", "📊 status":
		if callbacks.GetStatusFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetStatusFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "🟢 *Status:* Server Online & Proteksi Aktif.")
		}

	case "/stats", "stats", "📈 statistik":
		if callbacks.GetStatsSummaryFunc != nil {
			_ = b.SendMessage(chatIDStr, callbacks.GetStatsSummaryFunc())
		} else {
			_ = b.SendMessage(chatIDStr, "📊 *Statistik:* Statistik sedang dikumpulkan.")
		}

	case "/unblock":
		if len(parts) < 2 {
			_ = b.SendMessage(chatIDStr, "⚠️ *Format salah!*\nGunakan: `/unblock nama-domain.com` atau `/unblock tiktok`")
			return
		}
		target := cleanDomain(parts[1])

		// Check if target matches an official Blocked Service
		var matchedService *BlockedServiceItem
		if callbacks.GetAllAvailableServicesFunc != nil {
			for _, s := range callbacks.GetAllAvailableServicesFunc() {
				if strings.EqualFold(s.ID, target) || strings.EqualFold(s.Name, target) {
					matchedService = &s
					break
				}
			}
		}
		if matchedService != nil && callbacks.SetBlockedServiceFunc != nil {
			err := callbacks.SetBlockedServiceFunc(matchedService.ID, false)
			if err != nil {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("❌ *Gagal unblock layanan:* %s", err.Error()))
			} else {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("🟢 *Layanan %s (%s) BERHASIL DIIZINKAN!* ✅\n\nAkses ke %s telah dibuka kembali normal untuk seluruh jaringan.", matchedService.Name, matchedService.ID, matchedService.Name))
			}
			return
		}

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
			_ = b.SendMessage(chatIDStr, "⚠️ *Format salah!*\nGunakan: `/block nama-domain.com` atau `/block youtube`")
			return
		}
		target := cleanDomain(parts[1])

		// Check if target matches an official Blocked Service
		var matchedService *BlockedServiceItem
		if callbacks.GetAllAvailableServicesFunc != nil {
			for _, s := range callbacks.GetAllAvailableServicesFunc() {
				if strings.EqualFold(s.ID, target) || strings.EqualFold(s.Name, target) {
					matchedService = &s
					break
				}
			}
		}
		if matchedService != nil && callbacks.SetBlockedServiceFunc != nil {
			err := callbacks.SetBlockedServiceFunc(matchedService.ID, true)
			if err != nil {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("❌ *Gagal block layanan:* %s", err.Error()))
			} else {
				_ = b.SendMessage(chatIDStr, fmt.Sprintf("🔴 *Layanan %s (%s) BERHASIL DIBLOKIR!* 🔒\n\nSeluruh perangkat di jaringan sekarang diblokir dari mengakses %s.", matchedService.Name, matchedService.ID, matchedService.Name))
			}
			return
		}

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

	case "/pause", "pause", "⏸️ jeda filter":
		mins := 10
		if len(parts) >= 2 {
			if parsed, err := strconv.Atoi(parts[1]); err == nil && parsed > 0 {
				mins = parsed
			}
		}
		if callbacks.PauseProtectionFunc != nil {
			_ = callbacks.PauseProtectionFunc(mins)
		}
		_ = b.SendMessage(chatIDStr, fmt.Sprintf("⏸️ *Proteksi dijeda selama %d menit.*\nFilter otomatis aktif kembali setelah timer selesai, atau ketik `/resume`.", mins))

	case "/resume", "resume", "▶️ resume":
		if callbacks.ResumeProtectionFunc != nil {
			_ = callbacks.ResumeProtectionFunc()
		}
		_ = b.SendMessage(chatIDStr, "▶️ *Proteksi AdGuard Home kembali AKTIF!*")

	default:
		_ = b.SendInteractiveMenu(chatIDStr, fmt.Sprintf("❓ Perintah `%s` tidak dikenali.\n\nGunakan tombol menu di bawah untuk bernavigasi:", text))
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
