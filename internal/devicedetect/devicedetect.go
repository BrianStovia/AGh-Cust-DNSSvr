// Package devicedetect implements passive DNS fingerprinting and smart device
// profiling for AdGuard Home clients.
package devicedetect

import (
	"net/netip"
	"regexp"
	"strings"
	"sync"
	"time"
)

// DeviceType defines the category of detected hardware.
type DeviceType string

const (
	TypePhone        DeviceType = "phone"
	TypePC           DeviceType = "pc"
	TypeTV           DeviceType = "tv"
	TypeGameConsole  DeviceType = "gameconsole"
	TypeIoT          DeviceType = "iot"
	TypeNAS          DeviceType = "nas"
	TypeCamera       DeviceType = "camera"
	TypePrinter      DeviceType = "printer"
	TypeAudio        DeviceType = "audio"
	TypeOther        DeviceType = "other"
)

// DeviceInfo represents a profiled device discovered on the network.
type DeviceInfo struct {
	IP            string     `json:"ip"`
	ClientID      string     `json:"client_id,omitempty"`
	Name          string     `json:"name"`
	DeviceType    DeviceType `json:"device_type"`
	OS            string     `json:"os"`
	Vendor        string     `json:"vendor"`
	Model         string     `json:"model"`
	Icon          string     `json:"icon"` // apple, windows, android, tv, game, linux, iot, speaker, camera, printer, nas
	Confidence    int        `json:"confidence"` // 0 - 100
	MatchedRule   string     `json:"matched_rule"`
	MatchedDomain string     `json:"matched_domain"`
	FirstSeen     int64      `json:"first_seen"`
	LastSeen      int64      `json:"last_seen"`
	QueryCount    uint64     `json:"query_count"`
}

// FingerprintRule defines a domain or hostname pattern for device identification.
type FingerprintRule struct {
	Pattern       string
	IsSuffix      bool
	IsExact       bool
	Regex         *regexp.Regexp
	DeviceType    DeviceType
	OS            string
	Vendor        string
	Model         string
	Icon          string
	Confidence    int
	RuleName      string
}

// Detector manages passive DNS fingerprinting and device profiling.
type Detector struct {
	mu           sync.RWMutex
	devices      map[string]*DeviceInfo // key: IP or ClientID
	domainRules  []FingerprintRule
	hostRules    []FingerprintRule
}

var (
	detectorInstance *Detector
	detectorOnce     sync.Once
)

// GetDetector returns the singleton Detector instance.
func GetDetector() *Detector {
	detectorOnce.Do(func() {
		detectorInstance = newDetector()
	})
	return detectorInstance
}

func newDetector() *Detector {
	d := &Detector{
		devices: make(map[string]*DeviceInfo),
	}
	d.initRules()
	return d
}

func (d *Detector) initRules() {
	// Domain fingerprint rules (Captive portal probes, telemetry, cloud services, NTP)
	d.domainRules = []FingerprintRule{
		// --- APPLE ECOSYSTEM ---
		{Pattern: "captive.apple.com", IsExact: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (iPhone/Mac/iPad)", Icon: "apple", Confidence: 95, RuleName: "Apple Captive Portal"},
		{Pattern: "mask.icloud.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (iCloud Private Relay)", Icon: "apple", Confidence: 95, RuleName: "Apple iCloud Relay"},
		{Pattern: "mask-h2.icloud.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (iCloud Private Relay)", Icon: "apple", Confidence: 95, RuleName: "Apple iCloud Relay H2"},
		{Pattern: "gateway.icloud.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (iCloud Services)", Icon: "apple", Confidence: 90, RuleName: "Apple iCloud Gateway"},
		{Pattern: "smoot.apple.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (Siri/Spotlight)", Icon: "apple", Confidence: 90, RuleName: "Apple Siri Telemetry"},
		{Pattern: "time.apple.com", IsExact: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (NTP Time)", Icon: "apple", Confidence: 85, RuleName: "Apple Time Server"},
		{Pattern: "configuration.apple.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device", Icon: "apple", Confidence: 85, RuleName: "Apple Config"},
		{Pattern: "weather-data.apple.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (Weather)", Icon: "apple", Confidence: 85, RuleName: "Apple Weather"},
		{Pattern: "init.itunes.apple.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device (App Store)", Icon: "apple", Confidence: 80, RuleName: "Apple App Store"},
		{Pattern: ".apple.com", IsSuffix: true, DeviceType: TypePhone, OS: "Apple iOS/macOS", Vendor: "Apple", Model: "Apple Device", Icon: "apple", Confidence: 70, RuleName: "Apple Cloud"},

		// --- MICROSOFT / WINDOWS ---
		{Pattern: "dns.msftncsi.com", IsExact: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC / Laptop", Icon: "windows", Confidence: 95, RuleName: "Windows NCSI Probe"},
		{Pattern: "www.msftconnecttest.com", IsExact: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC / Laptop", Icon: "windows", Confidence: 95, RuleName: "Windows Connect Test"},
		{Pattern: "v10.events.data.microsoft.com", IsSuffix: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC / Laptop", Icon: "windows", Confidence: 90, RuleName: "Windows Telemetry (v10)"},
		{Pattern: "v20.events.data.microsoft.com", IsSuffix: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC / Laptop", Icon: "windows", Confidence: 90, RuleName: "Windows Telemetry (v20)"},
		{Pattern: "activity.windows.com", IsSuffix: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC / Laptop", Icon: "windows", Confidence: 90, RuleName: "Windows Activity Hub"},
		{Pattern: "slscr.update.microsoft.com", IsSuffix: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC (Windows Update)", Icon: "windows", Confidence: 90, RuleName: "Windows Update"},
		{Pattern: "settings-win.data.microsoft.com", IsSuffix: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC / Laptop", Icon: "windows", Confidence: 85, RuleName: "Windows Settings Sync"},
		{Pattern: "time.windows.com", IsExact: true, DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC (NTP)", Icon: "windows", Confidence: 85, RuleName: "Windows Time Service"},

		// --- ANDROID / GOOGLE ---
		{Pattern: "connectivitycheck.gstatic.com", IsExact: true, DeviceType: TypePhone, OS: "Android", Vendor: "Google", Model: "Android Smartphone / Tablet", Icon: "android", Confidence: 95, RuleName: "Android Captive Portal"},
		{Pattern: "connectivitycheck.android.com", IsExact: true, DeviceType: TypePhone, OS: "Android", Vendor: "Google", Model: "Android Smartphone / Tablet", Icon: "android", Confidence: 95, RuleName: "Android Check"},
		{Pattern: "clients3.google.com", IsSuffix: true, DeviceType: TypePhone, OS: "Android", Vendor: "Google", Model: "Android Device / ChromeOS", Icon: "android", Confidence: 80, RuleName: "Google Client Services"},
		{Pattern: "android.clients.google.com", IsSuffix: true, DeviceType: TypePhone, OS: "Android", Vendor: "Google", Model: "Android Smartphone / Tablet", Icon: "android", Confidence: 90, RuleName: "Android Services"},
		{Pattern: "device-provisioning.googleapis.com", IsSuffix: true, DeviceType: TypePhone, OS: "Android", Vendor: "Google", Model: "Android Device", Icon: "android", Confidence: 85, RuleName: "Android Device Provisioning"},

		// --- SAMSUNG SMART TV & GALAXY ---
		{Pattern: "samsungcloudsolution.com", IsSuffix: true, DeviceType: TypeTV, OS: "Tizen OS", Vendor: "Samsung", Model: "Samsung Smart TV", Icon: "tv", Confidence: 95, RuleName: "Samsung TV Cloud"},
		{Pattern: "samsungqbe.com", IsSuffix: true, DeviceType: TypeTV, OS: "Tizen OS", Vendor: "Samsung", Model: "Samsung Smart TV", Icon: "tv", Confidence: 95, RuleName: "Samsung QBE TV Hub"},
		{Pattern: "osb.samsungcloudsolution.com", IsSuffix: true, DeviceType: TypeTV, OS: "Tizen OS", Vendor: "Samsung", Model: "Samsung Smart TV", Icon: "tv", Confidence: 95, RuleName: "Samsung OSB TV"},
		{Pattern: "tvx.samsungcloudsolution.com", IsSuffix: true, DeviceType: TypeTV, OS: "Tizen OS", Vendor: "Samsung", Model: "Samsung Smart TV", Icon: "tv", Confidence: 95, RuleName: "Samsung TVX Cloud"},
		{Pattern: "vd.emp.samsungosp.com", IsSuffix: true, DeviceType: TypeTV, OS: "Tizen OS", Vendor: "Samsung", Model: "Samsung Smart TV", Icon: "tv", Confidence: 90, RuleName: "Samsung OSP Video"},
		{Pattern: "samsungcloudplatform.com", IsSuffix: true, DeviceType: TypePhone, OS: "Android / OneUI", Vendor: "Samsung", Model: "Samsung Galaxy Device", Icon: "android", Confidence: 85, RuleName: "Samsung Cloud Platform"},

		// --- LG WEBOS SMART TV ---
		{Pattern: "lgsmartad.com", IsSuffix: true, DeviceType: TypeTV, OS: "webOS", Vendor: "LG", Model: "LG Smart TV (webOS)", Icon: "tv", Confidence: 95, RuleName: "LG Smart TV Ad Hub"},
		{Pattern: "lgtvcommon.com", IsSuffix: true, DeviceType: TypeTV, OS: "webOS", Vendor: "LG", Model: "LG Smart TV (webOS)", Icon: "tv", Confidence: 95, RuleName: "LG TV Common"},
		{Pattern: "rdx2.lgtvsdp.com", IsSuffix: true, DeviceType: TypeTV, OS: "webOS", Vendor: "LG", Model: "LG Smart TV (webOS)", Icon: "tv", Confidence: 95, RuleName: "LG TV SDP"},
		{Pattern: "ibis.lgappstv.com", IsSuffix: true, DeviceType: TypeTV, OS: "webOS", Vendor: "LG", Model: "LG Smart TV (Apps)", Icon: "tv", Confidence: 95, RuleName: "LG Smart TV Apps"},

		// --- ROKU / STREAMING ---
		{Pattern: "sw.roku.com", IsSuffix: true, DeviceType: TypeTV, OS: "Roku OS", Vendor: "Roku", Model: "Roku Streaming Player / TV", Icon: "tv", Confidence: 95, RuleName: "Roku Software Update"},
		{Pattern: "api.roku.com", IsSuffix: true, DeviceType: TypeTV, OS: "Roku OS", Vendor: "Roku", Model: "Roku Streaming Player / TV", Icon: "tv", Confidence: 90, RuleName: "Roku API"},

		// --- GAMING CONSOLES ---
		{Pattern: "playstation.net", IsSuffix: true, DeviceType: TypeGameConsole, OS: "PlayStation OS", Vendor: "Sony", Model: "Sony PlayStation (PS4/PS5)", Icon: "game", Confidence: 95, RuleName: "PlayStation Network"},
		{Pattern: "ps5.ac.playstation.net", IsSuffix: true, DeviceType: TypeGameConsole, OS: "PlayStation OS", Vendor: "Sony", Model: "Sony PlayStation 5", Icon: "game", Confidence: 98, RuleName: "PS5 Network Core"},
		{Pattern: "ps4.ac.playstation.net", IsSuffix: true, DeviceType: TypeGameConsole, OS: "PlayStation OS", Vendor: "Sony", Model: "Sony PlayStation 4", Icon: "game", Confidence: 98, RuleName: "PS4 Network Core"},
		{Pattern: "conntest.nintendowifi.net", IsExact: true, DeviceType: TypeGameConsole, OS: "Nintendo Switch OS", Vendor: "Nintendo", Model: "Nintendo Switch Console", Icon: "game", Confidence: 95, RuleName: "Nintendo Wi-Fi Probe"},
		{Pattern: "ctest.cdn.nintendo.net", IsExact: true, DeviceType: TypeGameConsole, OS: "Nintendo Switch OS", Vendor: "Nintendo", Model: "Nintendo Switch Console", Icon: "game", Confidence: 95, RuleName: "Nintendo CDN Test"},
		{Pattern: "xsts.auth.xboxlive.com", IsSuffix: true, DeviceType: TypeGameConsole, OS: "Xbox OS", Vendor: "Microsoft", Model: "Microsoft Xbox Console", Icon: "game", Confidence: 95, RuleName: "Xbox Live Auth"},
		{Pattern: "title.msecnd.net", IsSuffix: true, DeviceType: TypeGameConsole, OS: "Xbox OS", Vendor: "Microsoft", Model: "Microsoft Xbox Console", Icon: "game", Confidence: 85, RuleName: "Xbox Content Delivery"},

		// --- SMART HOME & IOT ---
		{Pattern: "tuyaus.com", IsSuffix: true, DeviceType: TypeIoT, OS: "IoT Firmware", Vendor: "Tuya / SmartLife", Model: "Tuya Smart Home Device", Icon: "iot", Confidence: 95, RuleName: "Tuya Cloud US"},
		{Pattern: "tuyaeu.com", IsSuffix: true, DeviceType: TypeIoT, OS: "IoT Firmware", Vendor: "Tuya / SmartLife", Model: "Tuya Smart Home Device", Icon: "iot", Confidence: 95, RuleName: "Tuya Cloud EU"},
		{Pattern: "tuyacn.com", IsSuffix: true, DeviceType: TypeIoT, OS: "IoT Firmware", Vendor: "Tuya / SmartLife", Model: "Tuya Smart Home Device", Icon: "iot", Confidence: 95, RuleName: "Tuya Cloud CN"},
		{Pattern: "tplinkcloud.com", IsSuffix: true, DeviceType: TypeIoT, OS: "IoT Firmware", Vendor: "TP-Link", Model: "TP-Link / Tapo / Kasa Smart Device", Icon: "iot", Confidence: 95, RuleName: "TP-Link Cloud"},
		{Pattern: "kasa.tplink.com", IsSuffix: true, DeviceType: TypeIoT, OS: "IoT Firmware", Vendor: "TP-Link", Model: "Kasa Smart Home Device", Icon: "iot", Confidence: 95, RuleName: "TP-Link Kasa"},
		{Pattern: "miui.com", IsSuffix: true, DeviceType: TypePhone, OS: "HyperOS / MIUI", Vendor: "Xiaomi", Model: "Xiaomi / Redmi Smartphone", Icon: "android", Confidence: 90, RuleName: "Xiaomi MIUI Cloud"},
		{Pattern: "api.io.mi.com", IsSuffix: true, DeviceType: TypeIoT, OS: "Mijia IoT", Vendor: "Xiaomi", Model: "Xiaomi Mijia Smart Device", Icon: "iot", Confidence: 95, RuleName: "Xiaomi Mijia IoT"},
		{Pattern: "roborock.com", IsSuffix: true, DeviceType: TypeIoT, OS: "Roborock OS", Vendor: "Roborock", Model: "Roborock Robot Vacuum", Icon: "iot", Confidence: 95, RuleName: "Roborock Cloud"},

		// --- SMART AUDIO & SPEAKERS ---
		{Pattern: "pitangui.amazon.com", IsSuffix: true, DeviceType: TypeAudio, OS: "Fire OS", Vendor: "Amazon", Model: "Amazon Echo / Alexa Speaker", Icon: "speaker", Confidence: 95, RuleName: "Amazon Alexa Cloud"},
		{Pattern: "avs-alexa-na.amazon.com", IsSuffix: true, DeviceType: TypeAudio, OS: "Fire OS", Vendor: "Amazon", Model: "Amazon Echo / Alexa Speaker", Icon: "speaker", Confidence: 95, RuleName: "Amazon Alexa Voice"},
		{Pattern: "sonos.com", IsSuffix: true, DeviceType: TypeAudio, OS: "Sonos OS", Vendor: "Sonos", Model: "Sonos Smart Audio Speaker", Icon: "speaker", Confidence: 95, RuleName: "Sonos Audio Cloud"},
		{Pattern: "home.nest.com", IsSuffix: true, DeviceType: TypeAudio, OS: "Cast OS", Vendor: "Google", Model: "Google Nest / Home Speaker", Icon: "speaker", Confidence: 90, RuleName: "Google Nest Home"},

		// --- STORAGE & NAS ---
		{Pattern: "quickconnect.to", IsSuffix: true, DeviceType: TypeNAS, OS: "DSM (DiskStation)", Vendor: "Synology", Model: "Synology NAS Storage", Icon: "nas", Confidence: 95, RuleName: "Synology QuickConnect"},
		{Pattern: "checkport.synology.com", IsSuffix: true, DeviceType: TypeNAS, OS: "DSM (DiskStation)", Vendor: "Synology", Model: "Synology NAS Storage", Icon: "nas", Confidence: 95, RuleName: "Synology Port Check"},
		{Pattern: "myqnapcloud.com", IsSuffix: true, DeviceType: TypeNAS, OS: "QTS", Vendor: "QNAP", Model: "QNAP NAS Storage", Icon: "nas", Confidence: 95, RuleName: "QNAP Cloud"},

		// --- SECURITY CAMERAS ---
		{Pattern: "hik-connect.com", IsSuffix: true, DeviceType: TypeCamera, OS: "Embedded Linux", Vendor: "Hikvision", Model: "Hikvision IP Security Camera", Icon: "camera", Confidence: 95, RuleName: "Hikvision Cloud"},
		{Pattern: "ezvizlife.com", IsSuffix: true, DeviceType: TypeCamera, OS: "Embedded Linux", Vendor: "EZVIZ", Model: "EZVIZ Smart Security Camera", Icon: "camera", Confidence: 95, RuleName: "EZVIZ Cloud"},
		{Pattern: "reolink.com", IsSuffix: true, DeviceType: TypeCamera, OS: "Embedded Linux", Vendor: "Reolink", Model: "Reolink IP Camera", Icon: "camera", Confidence: 95, RuleName: "Reolink Cloud"},

		// --- PRINTERS ---
		{Pattern: "hpsmart.com", IsSuffix: true, DeviceType: TypePrinter, OS: "Printer Firmware", Vendor: "HP", Model: "HP Network Printer", Icon: "printer", Confidence: 95, RuleName: "HP Smart Print"},
		{Pattern: "epson.com", IsSuffix: true, DeviceType: TypePrinter, OS: "Printer Firmware", Vendor: "Epson", Model: "Epson Network Printer", Icon: "printer", Confidence: 90, RuleName: "Epson Connect"},

		// --- LINUX DISTRIBUTIONS ---
		{Pattern: "archive.ubuntu.com", IsSuffix: true, DeviceType: TypePC, OS: "Ubuntu Linux", Vendor: "Canonical", Model: "Ubuntu Workstation / Server", Icon: "linux", Confidence: 90, RuleName: "Ubuntu Archive"},
		{Pattern: "deb.debian.org", IsSuffix: true, DeviceType: TypePC, OS: "Debian Linux", Vendor: "Debian", Model: "Debian Workstation / Server", Icon: "linux", Confidence: 90, RuleName: "Debian Mirror"},
		{Pattern: "mirrors.fedoraproject.org", IsSuffix: true, DeviceType: TypePC, OS: "Fedora Linux", Vendor: "RedHat / Fedora", Model: "Fedora Linux PC", Icon: "linux", Confidence: 90, RuleName: "Fedora Mirror"},
		{Pattern: "archlinux.org", IsSuffix: true, DeviceType: TypePC, OS: "Arch Linux", Vendor: "Arch", Model: "Arch Linux PC", Icon: "linux", Confidence: 90, RuleName: "Arch Package Repo"},
	}

	// Hostname / rDNS signature rules
	d.hostRules = []FingerprintRule{
		{Regex: regexp.MustCompile(`(?i)iphone`), DeviceType: TypePhone, OS: "iOS", Vendor: "Apple", Model: "Apple iPhone", Icon: "apple", Confidence: 95, RuleName: "Hostname: iPhone"},
		{Regex: regexp.MustCompile(`(?i)ipad`), DeviceType: TypePhone, OS: "iPadOS", Vendor: "Apple", Model: "Apple iPad", Icon: "apple", Confidence: 95, RuleName: "Hostname: iPad"},
		{Regex: regexp.MustCompile(`(?i)(macbook|imac|mac-mini|mac-studio|macpro)`), DeviceType: TypePC, OS: "macOS", Vendor: "Apple", Model: "Apple Mac Computer", Icon: "apple", Confidence: 95, RuleName: "Hostname: Mac"},
		{Regex: regexp.MustCompile(`(?i)apple-watch`), DeviceType: TypeOther, OS: "watchOS", Vendor: "Apple", Model: "Apple Watch", Icon: "apple", Confidence: 95, RuleName: "Hostname: Apple Watch"},
		{Regex: regexp.MustCompile(`(?i)(desktop-|laptop-|win-|windows)`), DeviceType: TypePC, OS: "Windows 10/11", Vendor: "Microsoft", Model: "Windows PC", Icon: "windows", Confidence: 90, RuleName: "Hostname: Windows"},
		{Regex: regexp.MustCompile(`(?i)(galaxy|samsung)`), DeviceType: TypePhone, OS: "Android", Vendor: "Samsung", Model: "Samsung Galaxy Device", Icon: "android", Confidence: 90, RuleName: "Hostname: Galaxy"},
		{Regex: regexp.MustCompile(`(?i)(pixel|pixel-)`), DeviceType: TypePhone, OS: "Android", Vendor: "Google", Model: "Google Pixel Smartphone", Icon: "android", Confidence: 90, RuleName: "Hostname: Pixel"},
		{Regex: regexp.MustCompile(`(?i)(redmi|xiaomi|poco)`), DeviceType: TypePhone, OS: "Android / HyperOS", Vendor: "Xiaomi", Model: "Xiaomi / Redmi Device", Icon: "android", Confidence: 90, RuleName: "Hostname: Xiaomi"},
		{Regex: regexp.MustCompile(`(?i)(playstation|ps4|ps5)`), DeviceType: TypeGameConsole, OS: "PlayStation OS", Vendor: "Sony", Model: "Sony PlayStation", Icon: "game", Confidence: 95, RuleName: "Hostname: PlayStation"},
		{Regex: regexp.MustCompile(`(?i)(nintendo|switch)`), DeviceType: TypeGameConsole, OS: "Switch OS", Vendor: "Nintendo", Model: "Nintendo Switch", Icon: "game", Confidence: 95, RuleName: "Hostname: Switch"},
		{Regex: regexp.MustCompile(`(?i)xbox`), DeviceType: TypeGameConsole, OS: "Xbox OS", Vendor: "Microsoft", Model: "Xbox Console", Icon: "game", Confidence: 95, RuleName: "Hostname: Xbox"},
		{Regex: regexp.MustCompile(`(?i)(esp8266|esp32|tasmota|shelly|wled|sonoff)`), DeviceType: TypeIoT, OS: "MicroPython/C++", Vendor: "Espressif / OpenSource", Model: "Smart IoT Controller", Icon: "iot", Confidence: 95, RuleName: "Hostname: ESP/IoT"},
		{Regex: regexp.MustCompile(`(?i)(synology|diskstation)`), DeviceType: TypeNAS, OS: "DSM", Vendor: "Synology", Model: "Synology NAS", Icon: "nas", Confidence: 95, RuleName: "Hostname: Synology"},
		{Regex: regexp.MustCompile(`(?i)(bravia|smarttv|webos|tizen|roku)`), DeviceType: TypeTV, OS: "Smart TV OS", Vendor: "Smart TV", Model: "Smart Television", Icon: "tv", Confidence: 90, RuleName: "Hostname: Smart TV"},
	}
}

// InspectQuery analyzes a DNS question for device fingerprinting signatures.
func (d *Detector) InspectQuery(ip netip.Addr, clientID string, domain string) {
	if !ip.IsValid() && clientID == "" {
		return
	}

	key := ip.String()
	if clientID != "" {
		key = clientID
	}

	cleanDomain := strings.ToLower(strings.TrimSuffix(domain, "."))
	if cleanDomain == "" {
		return
	}

	for _, rule := range d.domainRules {
		matched := false
		if rule.IsExact && cleanDomain == rule.Pattern {
			matched = true
		} else if rule.IsSuffix {
			if strings.HasSuffix(cleanDomain, rule.Pattern) {
				matched = true
			}
		}

		if matched {
			d.updateDeviceProfile(key, ip.String(), clientID, rule, cleanDomain)
			return
		}
	}

	// If device exists, update query count and last seen
	d.mu.Lock()
	if dev, exists := d.devices[key]; exists {
		dev.QueryCount++
		dev.LastSeen = time.Now().Unix()
	}
	d.mu.Unlock()
}

// InspectHostname analyzes a resolved hostname (from rDNS, DHCP, or ARP) for device profile patterns.
func (d *Detector) InspectHostname(ip netip.Addr, clientID string, hostname string) {
	if hostname == "" {
		return
	}

	key := ip.String()
	if clientID != "" {
		key = clientID
	}

	for _, rule := range d.hostRules {
		if rule.Regex != nil && rule.Regex.MatchString(hostname) {
			d.updateDeviceProfile(key, ip.String(), clientID, rule, hostname)
			return
		}
	}
}

func (d *Detector) updateDeviceProfile(key, ip, clientID string, rule FingerprintRule, matchedPattern string) {
	now := time.Now().Unix()

	d.mu.Lock()
	defer d.mu.Unlock()

	dev, exists := d.devices[key]
	if !exists {
		name := rule.Model
		if clientID != "" {
			name = clientID + " (" + rule.Model + ")"
		}

		d.devices[key] = &DeviceInfo{
			IP:            ip,
			ClientID:      clientID,
			Name:          name,
			DeviceType:    rule.DeviceType,
			OS:            rule.OS,
			Vendor:        rule.Vendor,
			Model:         rule.Model,
			Icon:          rule.Icon,
			Confidence:    rule.Confidence,
			MatchedRule:   rule.RuleName,
			MatchedDomain: matchedPattern,
			FirstSeen:     now,
			LastSeen:      now,
			QueryCount:    1,
		}
		return
	}

	// Device exists: update profile if new confidence is higher or equal
	dev.QueryCount++
	dev.LastSeen = now
	if rule.Confidence >= dev.Confidence {
		dev.DeviceType = rule.DeviceType
		dev.OS = rule.OS
		dev.Vendor = rule.Vendor
		dev.Model = rule.Model
		dev.Icon = rule.Icon
		dev.Confidence = rule.Confidence
		dev.MatchedRule = rule.RuleName
		dev.MatchedDomain = matchedPattern
	}
}

// GetDevice returns the DeviceInfo for a specific IP or ClientID.
func (d *Detector) GetDevice(ip netip.Addr, clientID string) *DeviceInfo {
	d.mu.RLock()
	defer d.mu.RUnlock()

	if clientID != "" {
		if dev, exists := d.devices[clientID]; exists {
			clone := *dev
			return &clone
		}
	}

	if ip.IsValid() {
		if dev, exists := d.devices[ip.String()]; exists {
			clone := *dev
			return &clone
		}
	}

	return nil
}

// GetAllDevices returns all discovered device profiles.
func (d *Detector) GetAllDevices() []*DeviceInfo {
	d.mu.RLock()
	defer d.mu.RUnlock()

	list := make([]*DeviceInfo, 0, len(d.devices))
	for _, dev := range d.devices {
		clone := *dev
		list = append(list, &clone)
	}
	return list
}

// Clear resets the detected device cache.
func (d *Detector) Clear() {
	d.mu.Lock()
	defer d.mu.Unlock()
	d.devices = make(map[string]*DeviceInfo)
}
