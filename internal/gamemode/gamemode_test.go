package gamemode

import (
	"testing"
)

func TestGameModeCatalog(t *testing.T) {
	SetEnabled(true)
	if !IsEnabled() {
		t.Fatalf("expected game mode to be enabled")
	}

	testCases := []struct {
		domain   string
		expected bool
	}{
		{"mobilelegends.com", true},
		{"api.mobilelegends.com", true},
		{"play.riotgames.com", true},
		{"val.services.riotcdn.net", true},
		{"roblox.com", true},
		{"c.rbxcdn.com", true},
		{"steamcommunity.com", true},
		{"google.com", false},
		{"facebook.com", false},
		{"tiktok.com", false},
	}

	for _, tc := range testCases {
		res := IsGameDomain(tc.domain)
		if res != tc.expected {
			t.Errorf("IsGameDomain(%q) = %v; want %v", tc.domain, res, tc.expected)
		}
	}

	RecordGameQuery("mobilelegends.com", "192.168.1.50")
	st := GetStatus()
	if st.TotalAccelerated == 0 {
		t.Errorf("expected TotalAccelerated > 0")
	}

	msg := FormatTelegramMessage()
	if len(msg) == 0 {
		t.Errorf("expected non-empty message")
	}
	t.Logf("Telegram Game Mode card:\n%s", msg)
}
