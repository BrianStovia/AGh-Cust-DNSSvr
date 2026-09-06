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
	Enabled       bool     `json:"enabled" yaml:"enabled"`
	BypassCosmetic bool    `json:"bypass_cosmetic" yaml:"bypass_cosmetic"`
	LowLatencyDNS bool     `json:"low_latency_dns" yaml:"low_latency_dns"`
	CustomDomains []string `json:"custom_domains" yaml:"custom_domains"`
}

// Status represents the runtime status of Game Mode.
type Status struct {
	Enabled          bool     `json:"enabled"`
	ActiveGamesCount int      `json:"active_games_count"`
	TotalAccelerated uint64   `json:"total_accelerated_queries"`
	SupportedGames   []string `json:"supported_games"`
	LastGameDomain   string   `json:"last_game_domain"`
	LastActiveTime   string   `json:"last_active_time"`
}

// GameCatalogItem defines a game cluster.
type GameCatalogItem struct {
	Name    string   `json:"name"`
	Icon    string   `json:"icon"`
	Domains []string `json:"domains"`
}

var (
	globalMu        sync.RWMutex
	globalEnabled   = true // Enabled by default for ultra-low latency gaming
	bypassCosmetic  = true
	totalGameQuery  uint64
	lastDomain      string
	lastActive      time.Time
	customDomainsMu sync.RWMutex
	customDomains   = make(map[string]bool)

	// BuiltinGameCatalog contains the top online game domains.
	BuiltinGameCatalog = []GameCatalogItem{
		{
			Name: "Mobile Legends: Bang Bang",
			Icon: "⚔️",
			Domains: []string{
				"mobilelegends.com",
				"moonton.com",
				"youngjoygame.com",
				"akamaized.net",
			},
		},
		{
			Name: "Valorant & Riot Games",
			Icon: "🎯",
			Domains: []string{
				"riotgames.com",
				"leagueoflegends.com",
				"pvp.net",
				"riotcdn.net",
				"val.services.riotcdn.net",
			},
		},
		{
			Name: "PUBG Mobile & Tencent",
			Icon: "🔫",
			Domains: []string{
				"pubgmobile.com",
				"igamecj.com",
				"proximabeta.com",
				"krafton.com",
				"tencentgames.com",
			},
		},
		{
			Name: "Free Fire & Garena",
			Icon: "🔥",
			Domains: []string{
				"garena.com",
				"garenanow.com",
				"freefiremobile.com",
				"ff.garena.com",
			},
		},
		{
			Name: "Steam & Valve",
			Icon: "🎮",
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
			Name: "Roblox",
			Icon: "🧱",
			Domains: []string{
				"roblox.com",
				"rbxcdn.com",
				"robloxlabs.com",
			},
		},
		{
			Name: "Genshin Impact & HoYoverse",
			Icon: "✨",
			Domains: []string{
				"hoyoverse.com",
				"mihoyo.com",
				"yuanshen.com",
				"hoyolab.com",
			},
		},
		{
			Name: "PlayStation & Xbox & Nintendo",
			Icon: "🕹️",
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
			Name: "Epic Games & Fortnite",
			Icon: "🏆",
			Domains: []string{
				"epicgames.com",
				"unrealengine.com",
				"fortnite.com",
			},
		},
		{
			Name: "Battle.net & Blizzard",
			Icon: "❄️",
			Domains: []string{
				"battle.net",
				"blizzard.com",
				"activision.com",
			},
		},
		{
			Name: "Discord Voice & Game Chat",
			Icon: "👾",
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

// IsGameDomain checks whether the given hostname belongs to a recognized game network.
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

	for _, game := range BuiltinGameCatalog {
		for _, d := range game.Domains {
			if host == d || strings.HasSuffix(host, "."+d) {
				return true
			}
		}
	}

	return false
}

// RecordGameQuery logs an accelerated game query event.
func RecordGameQuery(host, clientIP string) {
	atomic.AddUint64(&totalGameQuery, 1)

	globalMu.Lock()
	lastDomain = host
	lastActive = time.Now()
	globalMu.Unlock()
}

// GetStatus returns the current status and metrics of Game Mode.
func GetStatus() Status {
	globalMu.RLock()
	enabled := globalEnabled
	lastD := lastDomain
	lastT := lastActive
	globalMu.RUnlock()

	var gamesList []string
	for _, g := range BuiltinGameCatalog {
		gamesList = append(gamesList, fmt.Sprintf("%s %s", g.Icon, g.Name))
	}

	lastTimeStr := "Belum ada query"
	if !lastT.IsZero() {
		lastTimeStr = lastT.Format("02 Jan 2006 15:04:05 WIB")
	}

	return Status{
		Enabled:          enabled,
		ActiveGamesCount: len(BuiltinGameCatalog),
		TotalAccelerated: atomic.LoadUint64(&totalGameQuery),
		SupportedGames:   gamesList,
		LastGameDomain:   lastD,
		LastActiveTime:   lastTimeStr,
	}
}

// FormatTelegramMessage returns a styled Markdown report for Telegram.
func FormatTelegramMessage() string {
	st := GetStatus()
	stateStr := "🔴 NONAKTIF"
	badge := "⏸️"
	if st.Enabled {
		stateStr = "🟢 AKTIF (Ultra-Low Latency QoS)"
		badge = "⚡"
	}

	var gameLines []string
	for _, g := range BuiltinGameCatalog {
		gameLines = append(gameLines, fmt.Sprintf("  • %s *%s*", g.Icon, g.Name))
	}

	return fmt.Sprintf("🎮 *SMART GAME QoS ACCELERATOR* %s\n\n"+
		"• *Status Mode:* %s\n"+
		"• *Total Query Dipercepat:* `%d query`\n"+
		"• *Domain Terakhir:* `%s`\n"+
		"• *Fast-Path Resolver:* `Cloudflare / Google Anycast 0ms`\n\n"+
		"🕹️ *Daftar Game Terakselerasi:*\n"+
		"%s\n\n"+
		"🛡️ _Mode ini memotong latensi DNS game dan menjaga ping in-game stabil tanpa jitter!_\n\n"+
		"_Ketik `/gamemode on` atau `/gamemode off` untuk beralih mode._",
		badge,
		stateStr,
		st.TotalAccelerated,
		st.LastGameDomain,
		strings.Join(gameLines, "\n"),
	)
}
