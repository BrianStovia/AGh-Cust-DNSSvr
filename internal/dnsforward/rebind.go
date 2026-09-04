package dnsforward

import (
	"net"
	"strings"
	"sync"
	"time"
)

// RebindEvent records a blocked DNS rebinding attack attempt.
type RebindEvent struct {
	Timestamp string `json:"timestamp"`
	Domain    string `json:"domain"`
	ClientIP  string `json:"client_ip"`
	TargetIP  string `json:"target_ip"`
	Action    string `json:"action"`
}

// RebindConfig holds the Anti-DNS Rebinding configuration and state.
type RebindConfig struct {
	Enabled            bool          `json:"enabled"`
	StrictMode         bool          `json:"strict_mode"`
	WhitelistedDomains []string      `json:"whitelisted_domains"`
	BlockedCount       uint64        `json:"blocked_count"`
	RecentAttacks      []RebindEvent `json:"recent_attacks"`
}

var (
	rebindMu   sync.RWMutex
	rebindConf = RebindConfig{
		Enabled:    true,
		StrictMode: true,
		WhitelistedDomains: []string{
			"local",
			"lan",
			"home",
			"home.arpa",
			"internal",
			"intranet",
			"corp",
			"lab",
			"plex.direct",
			"myfritz.net",
			"ts.net",
			"in-addr.arpa",
			"ip6.arpa",
		},
		BlockedCount:  0,
		RecentAttacks: []RebindEvent{},
	}
)

// GetRebindConfig returns a copy of current Anti-DNS Rebinding configuration.
func GetRebindConfig() RebindConfig {
	rebindMu.RLock()
	defer rebindMu.RUnlock()

	whitelisted := make([]string, len(rebindConf.WhitelistedDomains))
	copy(whitelisted, rebindConf.WhitelistedDomains)

	attacks := make([]RebindEvent, len(rebindConf.RecentAttacks))
	copy(attacks, rebindConf.RecentAttacks)

	return RebindConfig{
		Enabled:            rebindConf.Enabled,
		StrictMode:         rebindConf.StrictMode,
		WhitelistedDomains: whitelisted,
		BlockedCount:       rebindConf.BlockedCount,
		RecentAttacks:      attacks,
	}
}

// SetRebindConfig updates the Anti-DNS Rebinding configuration.
func SetRebindConfig(enabled bool, strictMode bool, whitelisted []string) RebindConfig {
	rebindMu.Lock()
	defer rebindMu.Unlock()

	rebindConf.Enabled = enabled
	rebindConf.StrictMode = strictMode

	cleaned := make([]string, 0, len(whitelisted))
	for _, w := range whitelisted {
		w = strings.TrimSpace(strings.ToLower(w))
		w = strings.TrimPrefix(w, "*.")
		w = strings.TrimPrefix(w, ".")
		if w != "" {
			cleaned = append(cleaned, w)
		}
	}
	rebindConf.WhitelistedDomains = cleaned

	return GetRebindConfig()
}

// ClearRebindStats resets the blocked count and recent attack logs.
func ClearRebindStats() {
	rebindMu.Lock()
	defer rebindMu.Unlock()

	rebindConf.BlockedCount = 0
	rebindConf.RecentAttacks = []RebindEvent{}
}

// isPrivateOrLocalIP checks if an IP belongs to private, loopback, link-local, or unspecified ranges.
func isPrivateOrLocalIP(ip net.IP) bool {
	if ip == nil {
		return false
	}

	if ip.IsLoopback() || ip.IsPrivate() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() || ip.IsUnspecified() {
		return true
	}

	// Also check 0.0.0.0/8
	if ip4 := ip.To4(); ip4 != nil {
		if ip4[0] == 0 {
			return true
		}
	}

	return false
}

// isDomainWhitelistedForRebind checks if the domain is in the allowed whitelist.
func isDomainWhitelistedForRebind(domain string, whitelist []string) bool {
	domain = strings.TrimSuffix(strings.ToLower(strings.TrimSpace(domain)), ".")
	for _, w := range whitelist {
		w = strings.TrimSuffix(strings.ToLower(strings.TrimSpace(w)), ".")
		if domain == w || strings.HasSuffix(domain, "."+w) {
			return true
		}
	}
	return false
}

// CheckDNSRebinding checks if the resolved IP for domain is a suspicious DNS Rebinding attempt.
// Returns true if it is an attack and should be blocked.
func CheckDNSRebinding(domain string, ip net.IP, clientIP string) bool {
	rebindMu.RLock()
	enabled := rebindConf.Enabled
	whitelist := rebindConf.WhitelistedDomains
	rebindMu.RUnlock()

	if !enabled {
		return false
	}

	if !isPrivateOrLocalIP(ip) {
		return false
	}

	// If domain is legitimately whitelisted, allow it.
	if isDomainWhitelistedForRebind(domain, whitelist) {
		return false
	}

	// This is a DNS Rebinding attack attempt!
	recordRebindAttack(domain, ip.String(), clientIP)
	return true
}

// recordRebindAttack logs the rebinding incident into recent attacks.
func recordRebindAttack(domain string, targetIP string, clientIP string) {
	rebindMu.Lock()
	defer rebindMu.Unlock()

	rebindConf.BlockedCount++

	evt := RebindEvent{
		Timestamp: time.Now().Format("2006-01-02 15:04:05"),
		Domain:    domain,
		ClientIP:  clientIP,
		TargetIP:  targetIP,
		Action:    "BLOCKED (Protected Local Network)",
	}

	// Keep max 50 recent attacks
	rebindConf.RecentAttacks = append([]RebindEvent{evt}, rebindConf.RecentAttacks...)
	if len(rebindConf.RecentAttacks) > 50 {
		rebindConf.RecentAttacks = rebindConf.RecentAttacks[:50]
	}
}
