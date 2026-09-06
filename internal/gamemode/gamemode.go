package gamemode

import (
	"fmt"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

// Config represents the Smart Game Mode QoS configuration.
type Config struct {
	Enabled        bool     `json:"enabled" yaml:"enabled"`
	Focus          string   `json:"focus" yaml:"focus"` // "all", "garena", "tencent"
	BypassCosmetic bool     `json:"bypass_cosmetic" yaml:"bypass_cosmetic"`
	LowLatencyDNS  bool     `json:"low_latency_dns" yaml:"low_latency_dns"`
	CustomDomains  []string `json:"custom_domains" yaml:"custom_domains"`
}

// Status represents the runtime status of Game Mode.
type Status struct {
	Enabled          bool     `json:"enabled"`
	FocusMode        string   `json:"focus_mode"` // "all", "garena", "tencent"
	ActiveGamesCount int      `json:"active_games_count"`
	TotalAccelerated uint64   `json:"total_accelerated_queries"`
	GarenaQueries    uint64   `json:"garena_queries"`
	TencentQueries   uint64   `json:"tencent_queries"`
	OtherGameQueries uint64   `json:"other_game_queries"`
	SupportedGames   []string `json:"supported_games"`
	LastGameDomain   string   `json:"last_game_domain"`
	LastGamePlatform string   `json:"last_game_platform"`
	LastActiveTime   string   `json:"last_active_time"`
}

// GameCatalogItem defines a game cluster.
type GameCatalogItem struct {
	ID       string   `json:"id"`
	Name     string   `json:"name"`
	Platform string   `json:"platform"` // "garena", "tencent", "general"
	Icon     string   `json:"icon"`
	Domains  []string `json:"domains"`
}

var (
	globalMu        sync.RWMutex
	globalEnabled   = true   // Enabled by default for ultra-low latency gaming
	globalFocus     = "all"  // "all", "garena", "tencent"
	totalGameQuery  uint64
	garenaQueryCnt  uint64
	tencentQueryCnt uint64
	otherQueryCnt   uint64
	lastDomain      string
	lastPlatform    string
	lastActive      time.Time
	customDomainsMu sync.RWMutex
	customDomains   = make(map[string]bool)

	// BuiltinGameCatalog contains comprehensive domain clusters with Garena & Tencent specialization.
	BuiltinGameCatalog = []GameCatalogItem{
		// ==================== GARENA ECOSYSTEM ====================
		{
			ID:       "garena_ff",
			Name:     "Garena Free Fire & Free Fire MAX",
			Platform: "garena",
			Icon:     "🔥",
			Domains: []string{
				"freefiremobile.com",
				"ff.garena.com",
				"freefire.com",
				"ffsupport.garena.com",
				"dl.freefiremobile.com",
				"cdn.freefiremobile.com",
				"ff.garena.co.id",
				"garenafreefire.com",
			},
		},
		{
			ID:       "garena_core",
			Name:     "Garena Platform & Matchmaking Engine",
			Platform: "garena",
			Icon:     "🔴",
			Domains: []string{
				"garena.com",
				"garenanow.com",
				"garena.co.id",
				"garena.sg",
				"garena.ph",
				"garena.vn",
				"garena.my",
				"garenapb.com",
				"connect.garena.com",
				"auth.garena.com",
				"gas.garena.com",
				"shop.garena.co.id",
				"cdn.garenanow.com",
			},
		},
		{
			ID:       "garena_aov_undawn",
			Name:     "Garena AoV, Undawn & CODM ID",
			Platform: "garena",
			Icon:     "🏹",
			Domains: []string{
				"aov.garena.co.id",
				"aov.garena.com",
				"undawn.garena.com",
				"undawn.game",
				"codm.garena.co.id",
			},
		},

		// ==================== TENCENT & LEVEL INFINITE ECOSYSTEM ====================
		{
			ID:       "tencent_pubgm",
			Name:     "Tencent PUBG Mobile & Lightspeed",
			Platform: "tencent",
			Icon:     "🔫",
			Domains: []string{
				"pubgmobile.com",
				"igamecj.com",
				"proximabeta.com",
				"cdnpubg.com",
				"pubg.com",
				"krafton.com",
				"lightspeed.tencent.com",
				"voip.pubgmobile.com",
			},
		},
		{
			ID:       "tencent_hok_infinite",
			Name:     "Tencent Honor of Kings & Level Infinite",
			Platform: "tencent",
			Icon:     "👑",
			Domains: []string{
				"honorofkings.com",
				"hok.proximabeta.com",
				"levelinfinite.com",
				"intlgame.com",
				"gvoice.intlgame.com",
				"gvoice.qq.com",
			},
		},
		{
			ID:       "tencent_cloud_games",
			Name:     "Tencent Cloud Gaming CDN & TiMi Studios",
			Platform: "tencent",
			Icon:     "☁️",
			Domains: []string{
				"tencentgames.com",
				"qcloudcdn.com",
				"qq.com",
				"gtimg.com",
				"idqqimg.com",
				"myqcloud.com",
				"timistudios.com",
				"callofduty.com",
				"activision.com",
			},
		},

		// ==================== OTHER POPULAR GAMES ====================
		{
			ID:       "moonton_mlbb",
			Name:     "Mobile Legends: Bang Bang",
			Platform: "general",
			Icon:     "⚔️",
			Domains: []string{
				"mobilelegends.com",
				"moonton.com",
				"youngjoygame.com",
				"akamaized.net",
			},
		},
		{
			ID:       "riot_games",
			Name:     "Valorant & Riot Games",
			Platform: "general",
			Icon:     "🎯",
			Domains: []string{
				"riotgames.com",
				"leagueoflegends.com",
				"pvp.net",
				"riotcdn.net",
				"val.services.riotcdn.net",
			},
		},
		{
			ID:       "valve_steam",
			Name:     "Steam & Valve Network",
			Platform: "general",
			Icon:     "🎮",
			Domains: []string{
				"steampowered.com",
				"steamcommunity.com",
				"steamstatic.com",
				"valve.net",
				"valvesoftware.com",
				"steamcontent.com",
			},
		},
		{
			ID:       "roblox",
			Name:     "Roblox",
			Platform: "general",
			Icon:     "🧱",
			Domains: []string{
				"roblox.com",
				"rbxcdn.com",
				"robloxlabs.com",
			},
		},
		{
			ID:       "hoyoverse",
			Name:     "Genshin Impact & HoYoverse",
			Platform: "general",
			Icon:     "✨",
			Domains: []string{
				"hoyoverse.com",
				"mihoyo.com",
				"yuanshen.com",
				"hoyolab.com",
			},
		},
		{
			ID:       "console_gaming",
			Name:     "PlayStation & Xbox & Nintendo",
			Platform: "general",
			Icon:     "🕹️",
			Domains: []string{
				"playstation.net",
				"playstation.com",
				"sonyentertainmentnetwork.com",
				"xboxlive.com",
				"xbox.com",
				"nintendo.net",
				"nintendo.com",
			},
		},
		{
			ID:       "discord_voice",
			Name:     "Discord Voice & Game Chat",
			Platform: "general",
			Icon:     "👾",
			Domains: []string{
				"discord.gg",
				"discord.media",
				"discordapp.com",
				"discord.com",
			},
		},
	}
)

// IsEnabled returns true if Game Mode QoS fast-path is active.
func IsEnabled() bool {
	globalMu.RLock()
	defer globalMu.RUnlock()
	return globalEnabled
}

// GetFocusMode returns the current focus mode: "all", "garena", or "tencent".
func GetFocusMode() string {
	globalMu.RLock()
	defer globalMu.RUnlock()
	return globalFocus
}

// SetFocusMode changes the focus mode.
func SetFocusMode(focus string) {
	globalMu.Lock()
	defer globalMu.Unlock()
	focus = strings.ToLower(strings.TrimSpace(focus))
	switch focus {
	case "garena", "ff", "freefire":
		globalFocus = "garena"
		globalEnabled = true
	case "tencent", "pubg", "pubgm", "hok":
		globalFocus = "tencent"
		globalEnabled = true
	default:
		globalFocus = "all"
	}
}

// SetEnabled enables or disables Game Mode QoS.
func SetEnabled(enabled bool) {
	globalMu.Lock()
	defer globalMu.Unlock()
	globalEnabled = enabled
}

// Toggle flips the Game Mode state and returns the new state.
func Toggle() bool {
	globalMu.Lock()
	defer globalMu.Unlock()
	globalEnabled = !globalEnabled
	return globalEnabled
}

// IsGameDomain checks whether the given hostname belongs to an active game network based on focus.
func IsGameDomain(host string) bool {
	if !IsEnabled() {
		return false
	}

	host = strings.ToLower(strings.TrimSuffix(strings.TrimSpace(host), "."))
	if host == "" {
		return false
	}

	customDomainsMu.RLock()
	if customDomains[host] {
		customDomainsMu.RUnlock()
		return true
	}
	customDomainsMu.RUnlock()

	focus := GetFocusMode()

	for _, game := range BuiltinGameCatalog {
		// Filter by focus if set
		if focus == "garena" && game.Platform != "garena" {
			continue
		}
		if focus == "tencent" && game.Platform != "tencent" {
			continue
		}

		for _, d := range game.Domains {
			if host == d || strings.HasSuffix(host, "."+d) {
				return true
			}
		}
	}

	return false
}

// RecordGameQuery logs an accelerated game query event and tracks Garena/Tencent counters.
func RecordGameQuery(host, clientIP string) {
	atomic.AddUint64(&totalGameQuery, 1)

	platform := "general"
	for _, game := range BuiltinGameCatalog {
		for _, d := range game.Domains {
			if host == d || strings.HasSuffix(host, "."+d) {
				platform = game.Platform
				break
			}
		}
		if platform != "general" {
			break
		}
	}

	switch platform {
	case "garena":
		atomic.AddUint64(&garenaQueryCnt, 1)
	case "tencent":
		atomic.AddUint64(&tencentQueryCnt, 1)
	default:
		atomic.AddUint64(&otherQueryCnt, 1)
	}

	globalMu.Lock()
	lastDomain = host
	lastPlatform = platform
	lastActive = time.Now()
	globalMu.Unlock()
}

// GetStatus returns the current status and metrics of Game Mode.
func GetStatus() Status {
	globalMu.RLock()
	enabled := globalEnabled
	focus := globalFocus
	lastD := lastDomain
	lastP := lastPlatform
	lastT := lastActive
	globalMu.RUnlock()

	var gamesList []string
	for _, g := range BuiltinGameCatalog {
		if focus == "garena" && g.Platform != "garena" {
			continue
		}
		if focus == "tencent" && g.Platform != "tencent" {
			continue
		}
		gamesList = append(gamesList, fmt.Sprintf("%s %s", g.Icon, g.Name))
	}

	lastTimeStr := "Belum ada query"
	if !lastT.IsZero() {
		lastTimeStr = lastT.Format("02 Jan 2006 15:04:05 WIB")
	}

	return Status{
		Enabled:          enabled,
		FocusMode:        focus,
		ActiveGamesCount: len(BuiltinGameCatalog),
		TotalAccelerated: atomic.LoadUint64(&totalGameQuery),
		GarenaQueries:    atomic.LoadUint64(&garenaQueryCnt),
		TencentQueries:   atomic.LoadUint64(&tencentQueryCnt),
		OtherGameQueries: atomic.LoadUint64(&otherQueryCnt),
		SupportedGames:   gamesList,
		LastGameDomain:   lastD,
		LastGamePlatform: lastP,
		LastActiveTime:   lastTimeStr,
	}
}

// FormatTelegramMessage returns a styled Markdown report for Telegram with Garena & Tencent focus.
func FormatTelegramMessage() string {
	st := GetStatus()
	stateStr := "🔴 NONAKTIF"
	badge := "⏸️"
	if st.Enabled {
		switch st.FocusMode {
		case "garena":
			stateStr = "🟢 AKTIF (⚡ Khusus Garena & Free Fire QoS)"
			badge = "🔥"
		case "tencent":
			stateStr = "🟢 AKTIF (⚡ Khusus Tencent & PUBG Mobile QoS)"
			badge = "🔫"
		default:
			stateStr = "🟢 AKTIF (⚡ Semua Game Online QoS)"
			badge = "🎮"
		}
	}

	focusLabel := "🌐 Semua Game"
	if st.FocusMode == "garena" {
		focusLabel = "🔥 Garena & Free Fire"
	} else if st.FocusMode == "tencent" {
		focusLabel = "🔫 Tencent & PUBG Mobile"
	}

	lastDom := st.LastGameDomain
	if lastDom == "" {
		lastDom = "-"
	}

	return fmt.Sprintf("🎮 *SMART GAME QoS ACCELERATOR* %s\n\n"+
		"• *Status Mode:* %s\n"+
		"• *Fokus Akselerasi:* *%s*\n"+
		"• *Total Query Dipercepat:* `%d query`\n"+
		"  ├ 🔥 *Garena (FF/AoV):* `%d query`\n"+
		"  ├ 🔫 *Tencent (PUBG/HoK):* `%d query`\n"+
		"  └ ⚔️ *Game Lainnya (MLBB/Steam):* `%d query`\n"+
		"• *Domain Terakhir:* `%s`\n"+
		"• *Fast-Path Resolver:* `Cloudflare 1.1.1.1 / Google 8.8.8.8 Anycast 0ms`\n\n"+
		"🕹️ *Daftar Server Terakselerasi:* \n"+
		"  ├ 🔥 *Garena:* Free Fire, Free Fire MAX, AoV, Undawn, Garena CDN, CODM\n"+
		"  ├ 🔫 *Tencent:* PUBG Mobile, Honor of Kings, TiMi Studios, Tencent Cloud, GVoice\n"+
		"  └ 🎮 *Lainnya:* MLBB (Moonton), Valorant (Riot), Steam, Roblox, Genshin\n\n"+
		"🛡️ _Koneksi server game langsung diprioritaskan untuk memotong latensi, menghilangkan jitter & anti-packet-loss!_\n\n"+
		"_Ketik perintah untuk ganti mode:_\n"+
		"• `/gamemode garena` — Fokus Free Fire & Garena\n"+
		"• `/gamemode tencent` — Fokus PUBG Mobile & Tencent\n"+
		"• `/gamemode all` — Akselerasi Semua Game\n"+
		"• `/gamemode off` — Nonaktifkan Game Mode",
		badge,
		stateStr,
		focusLabel,
		st.TotalAccelerated,
		st.GarenaQueries,
		st.TencentQueries,
		st.OtherGameQueries,
		lastDom,
	)
}
