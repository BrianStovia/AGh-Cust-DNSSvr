package gamemode

import (
	"testing"
)

func TestGameModeGarenaAndTencent(t *testing.T) {
	// Test Garena Focus Mode
	SetFocusMode("garena")
	if GetFocusMode() != "garena" {
		t.Fatalf("expected focus mode garena, got %s", GetFocusMode())
	}

	if !IsGameDomain("freefiremobile.com") {
		t.Errorf("expected freefiremobile.com to be game domain in garena focus")
	}
	if !IsGameDomain("auth.garena.com") {
		t.Errorf("expected auth.garena.com to be game domain in garena focus")
	}
	if IsGameDomain("roblox.com") {
		t.Errorf("expected roblox.com to NOT match in garena-only focus")
	}

	RecordGameQuery("freefiremobile.com", "192.168.1.10")

	// Test Tencent Focus Mode
	SetFocusMode("tencent")
	if GetFocusMode() != "tencent" {
		t.Fatalf("expected focus mode tencent, got %s", GetFocusMode())
	}

	if !IsGameDomain("pubgmobile.com") {
		t.Errorf("expected pubgmobile.com to be game domain in tencent focus")
	}
	if !IsGameDomain("honorofkings.com") {
		t.Errorf("expected honorofkings.com to be game domain in tencent focus")
	}
	if IsGameDomain("freefiremobile.com") {
		t.Errorf("expected freefiremobile.com to NOT match in tencent-only focus")
	}

	RecordGameQuery("pubgmobile.com", "192.168.1.20")

	// Test All Mode
	SetFocusMode("all")
	if !IsGameDomain("freefiremobile.com") || !IsGameDomain("pubgmobile.com") || !IsGameDomain("steampowered.com") {
		t.Errorf("expected all game domains to match in all mode")
	}

	st := GetStatus()
	if st.GarenaQueries == 0 || st.TencentQueries == 0 {
		t.Errorf("expected non-zero garena and tencent queries: garena=%d, tencent=%d", st.GarenaQueries, st.TencentQueries)
	}

	msg := FormatTelegramMessage()
	t.Logf("Formatted Telegram Message:\n%s", msg)
}
