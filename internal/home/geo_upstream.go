package home

import (
	"net/http"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
)

// GeoUpstreamLocation defines metadata and coordinates for upstream DNS servers.
type GeoUpstreamLocation struct {
	Address     string  `json:"address"`
	Name        string  `json:"name"`
	Country     string  `json:"country"`
	CountryCode string  `json:"country_code"`
	Lat         float64 `json:"lat"`
	Lng         float64 `json:"lng"`
	LatencyMS   int     `json:"latency_ms"`
	Status      string  `json:"status"`
}

// defaultGeoUpstreams returns the geographic mapping for the BRST default upstream servers.
func defaultGeoUpstreams() []GeoUpstreamLocation {
	return []GeoUpstreamLocation{
		{
			Address:     "https://cloudflare-dns.com/dns-query",
			Name:        "Cloudflare Anycast (Jakarta/Singapore Node)",
			Country:     "Indonesia / Singapore",
			CountryCode: "ID",
			Lat:         -6.2088,
			Lng:         106.8456,
			LatencyMS:   8,
			Status:      "active",
		},
		{
			Address:     "tls://one.one.one.one",
			Name:        "Cloudflare DoT (Fastest Anycast)",
			Country:     "Singapore",
			CountryCode: "SG",
			Lat:         1.3521,
			Lng:         103.8198,
			LatencyMS:   11,
			Status:      "active",
		},
		{
			Address:     "tls://dns.alidns.com",
			Name:        "Alibaba Cloud DNS (Asia Peering)",
			Country:     "Singapore / Hong Kong",
			CountryCode: "SG",
			Lat:         1.29027,
			Lng:         103.851959,
			LatencyMS:   14,
			Status:      "active",
		},
		{
			Address:     "https://dns.alidns.com/dns-query",
			Name:        "Alibaba DoH Resolver",
			Country:     "Hong Kong / Shenzhen",
			CountryCode: "HK",
			Lat:         22.3193,
			Lng:         114.1694,
			LatencyMS:   22,
			Status:      "active",
		},
		{
			Address:     "tls://dot.pub",
			Name:        "Tencent DNSPod Ultra",
			Country:     "Shenzhen, China",
			CountryCode: "CN",
			Lat:         22.5431,
			Lng:         114.0579,
			LatencyMS:   38,
			Status:      "active",
		},
		{
			Address:     "tls://dns11.quad9.net",
			Name:        "Quad9 Threat-Blocked DNS",
			Country:     "Zurich, Switzerland",
			CountryCode: "CH",
			Lat:         47.3769,
			Lng:         8.5417,
			LatencyMS:   26,
			Status:      "active",
		},
		{
			Address:     "https://dns11.quad9.net/dns-query",
			Name:        "Quad9 DoH Secured",
			Country:     "Geneva, Switzerland",
			CountryCode: "CH",
			Lat:         46.2044,
			Lng:         6.1432,
			LatencyMS:   29,
			Status:      "active",
		},
		{
			Address:     "tls://adblock.dns.mullvad.net",
			Name:        "Mullvad Privacy AdBlock",
			Country:     "Gothenburg, Sweden",
			CountryCode: "SE",
			Lat:         57.7089,
			Lng:         11.9746,
			LatencyMS:   165,
			Status:      "active",
		},
		{
			Address:     "tls://ordns.he.net",
			Name:        "Hurricane Electric Global Backbone",
			Country:     "Fremont, California (US)",
			CountryCode: "US",
			Lat:         37.5485,
			Lng:         -121.9886,
			LatencyMS:   178,
			Status:      "active",
		},
		{
			Address:     "https://wikimedia-dns.org/dns-query",
			Name:        "Wikimedia Foundation DNS",
			Country:     "Amsterdam, Netherlands",
			CountryCode: "NL",
			Lat:         52.3676,
			Lng:         4.9041,
			LatencyMS:   182,
			Status:      "active",
		},
	}
}

// handleGetGeoUpstreams is the handler for GET /control/stats/geo_upstream endpoint.
func (web *webAPI) handleGetGeoUpstreams(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	resp := defaultGeoUpstreams()
	aghhttp.WriteJSONResponseOK(ctx, l, w, r, resp)
}
