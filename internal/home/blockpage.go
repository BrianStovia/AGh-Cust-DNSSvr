package home

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"html/template"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/AdguardTeam/AdGuardHome/internal/aghhttp"
)

// UnblockRequest represents a user-submitted request to whitelist a blocked domain.
type UnblockRequest struct {
	ID        string    `json:"id"`
	Domain    string    `json:"domain"`
	ClientIP  string    `json:"client_ip"`
	Name      string    `json:"name"`
	Reason    string    `json:"reason"`
	Status    string    `json:"status"` // "pending", "approved", "rejected"
	CreatedAt time.Time `json:"created_at"`
}

var (
	unblockMu       sync.RWMutex
	unblockRequests = make([]*UnblockRequest, 0)
)

// generateUnblockID generates a unique random hex ID for an unblock request.
func generateUnblockID() string {
	b := make([]byte, 8)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}

// blockPageTemplateHTML is the modern, responsive, dark-mode custom block page.
const blockPageTemplateHTML = `<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Akses Dibatasi - DNS SERVER BRST</title>
    <style>
        :root {
            --bg-base: #070b14;
            --bg-card: rgba(16, 24, 40, 0.85);
            --border-color: rgba(0, 212, 239, 0.25);
            --primary: #00d4ef;
            --primary-hover: #38bdf8;
            --danger: #ef4444;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --accent-glow: rgba(0, 212, 239, 0.15);
        }
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
        }
        body {
            background-color: var(--bg-base);
            background-image: 
                radial-gradient(circle at 50% 10%, rgba(239, 68, 68, 0.08) 0%, transparent 60%),
                radial-gradient(circle at 90% 90%, rgba(0, 212, 239, 0.06) 0%, transparent 50%),
                linear-gradient(rgba(15, 23, 42, 0.6) 1px, transparent 1px),
                linear-gradient(90deg, rgba(15, 23, 42, 0.6) 1px, transparent 1px);
            background-size: 100% 100%, 100% 100%, 40px 40px, 40px 40px;
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }
        .container {
            max-width: 580px;
            width: 100%;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 20px;
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6), 0 0 35px var(--accent-glow);
            overflow: hidden;
            animation: fadeIn 0.4s ease-out;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(15px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .header {
            padding: 32px 32px 20px;
            text-align: center;
            border-bottom: 1px solid rgba(255, 255, 255, 0.06);
        }
        .shield-icon {
            width: 72px;
            height: 72px;
            margin: 0 auto 16px;
            background: rgba(239, 68, 68, 0.12);
            border: 1px solid rgba(239, 68, 68, 0.35);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--danger);
            box-shadow: 0 0 24px rgba(239, 68, 68, 0.25);
        }
        .badge-brand {
            display: inline-block;
            padding: 4px 12px;
            background: rgba(0, 212, 239, 0.1);
            border: 1px solid rgba(0, 212, 239, 0.3);
            border-radius: 9999px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.08em;
            color: var(--primary);
            text-transform: uppercase;
            margin-bottom: 12px;
        }
        .title {
            font-size: 24px;
            font-weight: 700;
            letter-spacing: -0.02em;
            margin-bottom: 8px;
        }
        .subtitle {
            font-size: 14px;
            color: var(--text-muted);
            line-height: 1.6;
        }
        .content {
            padding: 24px 32px;
        }
        .domain-card {
            background: rgba(10, 15, 29, 0.7);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 12px;
            padding: 16px;
            margin-bottom: 24px;
        }
        .meta-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 6px 0;
            font-size: 13px;
        }
        .meta-row:not(:last-child) {
            border-bottom: 1px solid rgba(255, 255, 255, 0.04);
        }
        .meta-label {
            color: var(--text-muted);
        }
        .meta-value {
            font-weight: 600;
            color: var(--text-main);
            word-break: break-all;
        }
        .meta-domain {
            color: #f87171;
            font-family: monospace;
            font-size: 14px;
        }
        .form-section {
            background: rgba(15, 23, 42, 0.4);
            border: 1px solid rgba(0, 212, 239, 0.15);
            border-radius: 14px;
            padding: 20px;
        }
        .form-title {
            font-size: 15px;
            font-weight: 600;
            margin-bottom: 6px;
            display: flex;
            align-items: center;
            gap: 8px;
            color: var(--primary);
        }
        .form-desc {
            font-size: 12.5px;
            color: var(--text-muted);
            margin-bottom: 16px;
            line-height: 1.5;
        }
        .form-group {
            margin-bottom: 14px;
        }
        label {
            display: block;
            font-size: 12px;
            font-weight: 500;
            color: var(--text-muted);
            margin-bottom: 6px;
        }
        input, textarea {
            width: 100%;
            padding: 10px 14px;
            background: rgba(7, 11, 20, 0.8);
            border: 1px solid rgba(255, 255, 255, 0.12);
            border-radius: 8px;
            color: var(--text-main);
            font-size: 13px;
            outline: none;
            transition: all 0.2s;
        }
        input:focus, textarea:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(0, 212, 239, 0.15);
        }
        textarea {
            resize: vertical;
            min-height: 60px;
        }
        .submit-btn {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #0284c7 0%, #00d4ef 100%);
            border: none;
            border-radius: 8px;
            color: #031326;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
        }
        .submit-btn:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 15px rgba(0, 212, 239, 0.35);
        }
        .submit-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
        .alert {
            padding: 12px;
            border-radius: 8px;
            font-size: 13px;
            margin-top: 14px;
            display: none;
            animation: fadeIn 0.3s ease;
        }
        .alert-success {
            background: rgba(34, 197, 94, 0.15);
            border: 1px solid rgba(34, 197, 94, 0.35);
            color: #4ade80;
        }
        .footer {
            padding: 16px 32px 24px;
            text-align: center;
            font-size: 12px;
            color: #64748b;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="shield-icon">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
            </div>
            <span class="badge-brand">DNS SERVER BRST SECURITY</span>
            <h1 class="title">Akses Situs Dibatasi</h1>
            <p class="subtitle">Situs ini telah diblokir oleh kebijakan keamanan atau daftar filter perlindungan DNS SERVER BRST.</p>
        </div>

        <div class="content">
            <div class="domain-card">
                <div class="meta-row">
                    <span class="meta-label">Domain yang Dituju:</span>
                    <span class="meta-value meta-domain" id="targetDomain">{{.Domain}}</span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Alamat IP Klien:</span>
                    <span class="meta-value">{{.ClientIP}}</span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Kategori / Alasan:</span>
                    <span class="meta-value" style="color: #fbbf24;">Kebijakan Keamanan / Iklan &amp; Pelacak</span>
                </div>
            </div>

            <div class="form-section">
                <h3 class="form-title">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
                    </svg>
                    Ajukan Pembukaan Blokir (Request Unblock)
                </h3>
                <p class="form-desc">Jika Anda merasa situs ini salah diblokir (false positive) atau dibutuhkan untuk pekerjaan/keperluan mendesak, kirimkan permohonan ke Administrator.</p>
                
                <form id="unblockForm">
                    <div class="form-group">
                        <label for="userName">Nama / Identitas Anda:</label>
                        <input type="text" id="userName" placeholder="Contoh: Laptop Brian / Tim IT" required>
                    </div>
                    <div class="form-group">
                        <label for="userReason">Alasan Pembukaan Blokir (Opsional):</label>
                        <textarea id="userReason" placeholder="Contoh: Dibutuhkan untuk tugas kuliah / referensi pekerjaan"></textarea>
                    </div>
                    <button type="submit" class="submit-btn" id="submitBtn">
                        Kirim Permintaan ke Admin
                    </button>
                    <div class="alert alert-success" id="successAlert">
                        ✓ Permintaan buka blokir berhasil dikirim ke Administrator DNS SERVER BRST.
                    </div>
                </form>
            </div>
        </div>

        <div class="footer">
            DNS SERVER BRST &copy; 2026 by Brian Stovia &middot; Proteksi Jaringan Tingkat Lanjut
        </div>
    </div>

    <script>
        // Extract domain from query parameter if present
        const urlParams = new URLSearchParams(window.location.search);
        const paramDomain = urlParams.get('domain');
        if (paramDomain) {
            document.getElementById('targetDomain').textContent = paramDomain;
        }

        const form = document.getElementById('unblockForm');
        const submitBtn = document.getElementById('submitBtn');
        const successAlert = document.getElementById('successAlert');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            submitBtn.disabled = true;
            submitBtn.textContent = 'Mengirim Permintaan...';

            const domain = document.getElementById('targetDomain').textContent.trim();
            const name = document.getElementById('userName').value.trim();
            const reason = document.getElementById('userReason').value.trim();

            try {
                const res = await fetch('/control/unblock_requests/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ domain, name, reason })
                });

                if (res.ok) {
                    successAlert.style.display = 'block';
                    form.reset();
                    submitBtn.textContent = 'Permintaan Terkirim';
                } else {
                    alert('Gagal mengirim permintaan. Silakan coba lagi.');
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Kirim Permintaan ke Admin';
                }
            } catch (err) {
                alert('Terjadi kesalahan koneksi saat mengirim permintaan.');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Kirim Permintaan ke Admin';
            }
        });
    </script>
</body>
</html>`

// getClientIP extracts real client IP from request.
func getClientIP(r *http.Request) string {
	if fwd := r.Header.Get("X-Forwarded-For"); fwd != "" {
		parts := strings.Split(fwd, ",")
		return strings.TrimSpace(parts[0])
	}
	if realIP := r.Header.Get("X-Real-IP"); realIP != "" {
		return strings.TrimSpace(realIP)
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err == nil {
		return host
	}
	return r.RemoteAddr
}

// handleGetBlockPage serves the custom interactive branded block page.
func (web *webAPI) handleGetBlockPage(w http.ResponseWriter, r *http.Request) {
	domain := r.URL.Query().Get("domain")
	if domain == "" {
		host := r.Host
		if strings.Contains(host, ":") {
			host, _, _ = net.SplitHostPort(host)
		}
		domain = host
	}

	clientIP := getClientIP(r)

	data := struct {
		Domain   string
		ClientIP string
	}{
		Domain:   domain,
		ClientIP: clientIP,
	}

	tmpl, err := template.New("blockpage").Parse(blockPageTemplateHTML)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	_ = tmpl.Execute(w, data)
}

// handlePostSubmitUnblockRequest receives unblock submissions from blocked clients (public).
func (web *webAPI) handlePostSubmitUnblockRequest(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		Domain string `json:"domain"`
		Name   string `json:"name"`
		Reason string `json:"reason"`
	}

	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "reading req: %s", err)
		return
	}

	req.Domain = strings.TrimSpace(req.Domain)
	if req.Domain == "" {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "domain must not be empty")
		return
	}

	unblockReq := &UnblockRequest{
		ID:        generateUnblockID(),
		Domain:    req.Domain,
		ClientIP:  getClientIP(r),
		Name:      req.Name,
		Reason:    req.Reason,
		Status:    "pending",
		CreatedAt: time.Now().UTC(),
	}

	unblockMu.Lock()
	unblockRequests = append([]*UnblockRequest{unblockReq}, unblockRequests...)
	// Keep maximum 100 recent requests
	if len(unblockRequests) > 100 {
		unblockRequests = unblockRequests[:100]
	}
	unblockMu.Unlock()

	l.InfoContext(ctx, "received unblock request",
		"domain", unblockReq.Domain,
		"name", unblockReq.Name,
		"client_ip", unblockReq.ClientIP,
	)

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{
		"status":  "ok",
		"message": "Permintaan buka blokir berhasil dicatat",
		"id":      unblockReq.ID,
	})
}

// handleGetUnblockRequests returns the list of pending unblock requests (admin only).
func (web *webAPI) handleGetUnblockRequests(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	unblockMu.RLock()
	list := make([]*UnblockRequest, len(unblockRequests))
	copy(list, unblockRequests)
	unblockMu.RUnlock()

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, list)
}

// handlePostApproveUnblockRequest approves an unblock request and adds whitelist rule (admin only).
func (web *webAPI) handlePostApproveUnblockRequest(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		ID     string `json:"id"`
		Domain string `json:"domain"`
	}

	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "reading req: %s", err)
		return
	}

	req.Domain = strings.TrimSpace(req.Domain)
	if req.Domain == "" {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "domain must not be empty")
		return
	}

	// Add whitelist rule: @@||domain^$important
	whitelistRule := fmt.Sprintf("@@||%s^$important", req.Domain)
	if globalContext.filters != nil {
		globalContext.filters.AddUserRule(ctx, whitelistRule)
	}

	unblockMu.Lock()
	for _, item := range unblockRequests {
		if item.ID == req.ID || item.Domain == req.Domain {
			item.Status = "approved"
		}
	}
	unblockMu.Unlock()

	l.InfoContext(ctx, "approved unblock request and added whitelist rule",
		"domain", req.Domain,
		"rule", whitelistRule,
	)

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{
		"status":  "ok",
		"message": fmt.Sprintf("Domain %s berhasil dibuka blokirnya", req.Domain),
		"rule":    whitelistRule,
	})
}

// handlePostDeleteUnblockRequest deletes/rejects an unblock request (admin only).
func (web *webAPI) handlePostDeleteUnblockRequest(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	l := web.logger

	var req struct {
		ID string `json:"id"`
	}

	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		aghhttp.ErrorAndLog(ctx, l, r, w, http.StatusBadRequest, "reading req: %s", err)
		return
	}

	unblockMu.Lock()
	newList := make([]*UnblockRequest, 0, len(unblockRequests))
	for _, item := range unblockRequests {
		if item.ID != req.ID {
			newList = append(newList, item)
		}
	}
	unblockRequests = newList
	unblockMu.Unlock()

	aghhttp.WriteJSONResponseOK(ctx, l, w, r, map[string]any{
		"status": "ok",
	})
}
