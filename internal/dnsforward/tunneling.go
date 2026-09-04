package dnsforward

import (
	"math"
	"strings"
	"unicode"
)

// shannonEntropy calculates the Shannon entropy of a string.
// Higher entropy indicates random/encrypted/encoded binary data (Base32/Base64/Hex).
func shannonEntropy(s string) float64 {
	if len(s) == 0 {
		return 0
	}
	freq := make(map[rune]float64)
	for _, r := range s {
		freq[r]++
	}
	var entropy float64
	length := float64(len(s))
	for _, count := range freq {
		p := count / length
		entropy -= p * math.Log2(p)
	}
	return entropy
}

// isDNSTunneling detects potential DNS tunneling and data exfiltration patterns.
func isDNSTunneling(host string, _ uint16) (suspicious bool, reason string) {
	// Whitelist common legitimate long domains (e.g. email DKIM, reverse DNS, CDNs)
	if strings.Contains(host, "._domainkey.") ||
		strings.Contains(host, "domainkey") ||
		strings.HasSuffix(host, ".in-addr.arpa") ||
		strings.HasSuffix(host, ".ip6.arpa") ||
		strings.HasSuffix(host, ".akamaiedge.net") ||
		strings.HasSuffix(host, ".cloudfront.net") ||
		strings.HasSuffix(host, ".akadns.net") ||
		strings.HasSuffix(host, ".trafficmanager.net") {
		return false, ""
	}

	labels := strings.Split(host, ".")
	if len(labels) == 0 {
		return false, ""
	}

	for _, label := range labels {
		labelLen := len(label)

		// 1. Extreme single label length (>= 48 characters) with high randomness
		if labelLen >= 48 {
			ent := shannonEntropy(label)
			if ent >= 3.6 {
				return true, "DNS Tunneling: Exfiltration Payload Detected"
			}
		}

		// 2. High entropy hex/base64 encoded label (>= 32 chars)
		if labelLen >= 32 {
			digitOrHexCount := 0
			for _, r := range label {
				if unicode.IsDigit(r) || (r >= 'a' && r <= 'f') || (r >= 'A' && r <= 'F') {
					digitOrHexCount++
				}
			}
			ent := shannonEntropy(label)
			if ent >= 3.15 && float64(digitOrHexCount)/float64(labelLen) > 0.8 {
				return true, "DNS Tunneling: Encoded Hex Data"
			}
		}
	}

	// 3. Excessively long overall domain (>= 140 chars) with high entropy
	if len(host) >= 140 && len(labels) >= 4 {
		totalEntropy := shannonEntropy(host)
		if totalEntropy >= 4.0 {
			return true, "DNS Tunneling: Suspicious Long FQDN"
		}
	}

	return false, ""
}
