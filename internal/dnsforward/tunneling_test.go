package dnsforward

import (
	"testing"
)

func TestDNSTunneling(t *testing.T) {
	testCases := []struct {
		host       string
		qtype      uint16
		shouldDrop bool
		name       string
	}{
		{
			host:       "google.com",
			qtype:      1,
			shouldDrop: false,
			name:       "Standard legitimate domain",
		},
		{
			host:       "api.github.com",
			qtype:      1,
			shouldDrop: false,
			name:       "Standard subdomain",
		},
		{
			host:       "default._domainkey.example.com",
			qtype:      16,
			shouldDrop: false,
			name:       "Legitimate DKIM TXT record",
		},
		{
			host:       "4.3.2.1.in-addr.arpa",
			qtype:      12,
			shouldDrop: false,
			name:       "Reverse DNS pointer",
		},
		{
			host:       "d39f8a2b84920194cba829402948291048290148.dnstunnel.attacker.com",
			qtype:      1,
			shouldDrop: true,
			name:       "Exfiltration Hex Tunneling payload",
		},
		{
			host:       "aW5pdGlhbC1kYXRhLWV4ZmlsdHJhdGlvbi1wYXlsb2FkLXZpYS1kbnMtdHVubmVs.tunnel.c2.io",
			qtype:      1,
			shouldDrop: true,
			name:       "Base64 long encoded tunneling label",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			suspicious, reason := isDNSTunneling(tc.host, tc.qtype)
			if suspicious != tc.shouldDrop {
				t.Fatalf("for %q, expected suspicious=%v, got %v (reason: %s)", tc.host, tc.shouldDrop, suspicious, reason)
			}
		})
	}
}
