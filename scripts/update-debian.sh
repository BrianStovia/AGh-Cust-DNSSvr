#!/bin/sh

# ==============================================================================
# Auto-Updater for DNS SERVER BRST on Debian-based Linux Systems
# (Debian, Ubuntu, Raspberry Pi OS, Pop!_OS, Linux Mint)
# ==============================================================================

set -e -u

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "${CYAN}=====================================================${NC}"
echo "${CYAN}      DNS SERVER BRST - Auto Updater (Debian)        ${NC}"
echo "${CYAN}=====================================================${NC}"

# 1. Root privilege check
if [ "$(id -u)" -ne 0 ]; then
	echo "${RED}Error: Skrip updater ini harus dijalankan dengan akses root (sudo).${NC}" 1>&2
	exit 1
fi

INSTALL_DIR="/opt/AdGuardHome"

# 2. Check if installed
if [ ! -d "$INSTALL_DIR" ]; then
	echo "${YELLOW}[!] Direktori instalasi $INSTALL_DIR tidak ditemukan.${NC}"
	echo "${BLUE}Membuat direktori $INSTALL_DIR...${NC}"
	mkdir -p "$INSTALL_DIR"
fi

# 3. Backup Configuration
echo "${BLUE}[1/5] Membackup konfigurasi lama...${NC}"
if [ -f "$INSTALL_DIR/AdGuardHome.yaml" ]; then
	cp "$INSTALL_DIR/AdGuardHome.yaml" "$INSTALL_DIR/AdGuardHome.yaml.bak"
	echo "${GREEN}[✓] Konfigurasi berhasil dibackup ke $INSTALL_DIR/AdGuardHome.yaml.bak${NC}"
fi

# 4. Stop Service
echo "${BLUE}[2/5] Menghentikan service DNS SERVER BRST...${NC}"
if command -v systemctl >/dev/null 2>&1; then
	systemctl stop AdGuardHome >/dev/null 2>&1 || true
else
	"$INSTALL_DIR/AdGuardHome" -s stop >/dev/null 2>&1 || true
fi

# 5. Update Binary
echo "${BLUE}[3/5] Memperbarui biner DNS SERVER BRST...${NC}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UPDATED=0

if [ -f "$SCRIPT_DIR/../AdGuardHome" ]; then
	cp "$SCRIPT_DIR/../AdGuardHome" "$INSTALL_DIR/AdGuardHome"
	chmod +x "$INSTALL_DIR/AdGuardHome"
	UPDATED=1
	echo "${GREEN}[✓] Biner kustom lokal berhasil dipasang.${NC}"
elif [ -f "./AdGuardHome" ]; then
	cp "./AdGuardHome" "$INSTALL_DIR/AdGuardHome"
	chmod +x "$INSTALL_DIR/AdGuardHome"
	UPDATED=1
	echo "${GREEN}[✓] Biner kustom lokal berhasil dipasang.${NC}"
fi

if [ "$UPDATED" -eq 0 ]; then
	echo "${BLUE}Mengunduh paket biner DNS SERVER BRST terbaru dari GitHub...${NC}"
	ARCH="$(uname -m)"
	case "$ARCH" in
	x86_64) ARCH_TYPE="amd64" ;;
	aarch64 | arm64) ARCH_TYPE="arm64" ;;
	armv7l | armv7 | armhf) ARCH_TYPE="armv7" ;;
	*) ARCH_TYPE="amd64" ;;
	esac

	DOWNLOAD_URL="https://raw.githubusercontent.com/BrianStovia/AGh-Cust-DNSSvr/main/dist/AdGuardHome_linux_${ARCH_TYPE}"
	TMP_BIN="/tmp/AdGuardHome_custom_update_$$"

	if command -v curl >/dev/null 2>&1; then
		curl -sSL "$DOWNLOAD_URL" -o "$TMP_BIN"
	elif command -v wget >/dev/null 2>&1; then
		wget -qO "$TMP_BIN" "$DOWNLOAD_URL"
	else
		echo "${RED}Error: curl atau wget dibutuhkan untuk mengunduh biner.${NC}" 1>&2
		exit 1
	fi

	# Validate downloaded binary
	if [ ! -s "$TMP_BIN" ]; then
		echo "${RED}Error: Gagal mengunduh biner dari $DOWNLOAD_URL (berkas kosong).${NC}" 1>&2
		rm -f "$TMP_BIN"
		exit 1
	fi

	chmod +x "$TMP_BIN"

	# Test execution
	if ! "$TMP_BIN" --version >/dev/null 2>&1; then
		echo "${YELLOW}[!] Peringatan: Validasi biner gagal. Memasang dengan penanganan khusus...${NC}"
	fi

	cp -f "$TMP_BIN" "$INSTALL_DIR/AdGuardHome"
	chmod 755 "$INSTALL_DIR/AdGuardHome"
	rm -f "$TMP_BIN"
	echo "${GREEN}[✓] Biner kustom terbaru berhasil dipasang ke $INSTALL_DIR/AdGuardHome.${NC}"
fi

# 6. Re-apply Debian Kernel Performance Tuning
echo "${BLUE}[4/5] Menerapkan optimasi performa kernel sysctl & port 53...${NC}"
SYSCTL_CONF="/etc/sysctl.d/99-dns-server-brst.conf"
cat << 'EOF' > "$SYSCTL_CONF"
# DNS SERVER BRST Performance Optimizations
net.core.rmem_max = 8388608
net.core.wmem_max = 8388608
net.core.rmem_default = 1048576
net.core.wmem_default = 1048576
net.ipv4.udp_rmem_min = 16384
net.ipv4.udp_wmem_min = 16384
net.core.netdev_max_backlog = 10000
net.core.somaxconn = 4096
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_fastopen = 3
net.ipv4.tcp_fin_timeout = 15
EOF

if command -v sysctl >/dev/null 2>&1; then
	sysctl -p "$SYSCTL_CONF" >/dev/null 2>&1 || sysctl --system >/dev/null 2>&1 || true
fi

# Ensure systemd-resolved and other DNS daemons do not block port 53
if command -v systemctl >/dev/null 2>&1; then
	if systemctl is-active --quiet systemd-resolved 2>/dev/null || systemctl is-enabled systemd-resolved >/dev/null 2>&1; then
		mkdir -p /etc/systemd/resolved.conf.d
		cat << 'EOF' > /etc/systemd/resolved.conf.d/adguardhome.conf
[Resolve]
DNS=127.0.0.1
DNSStubListener=no
EOF
		if [ -f /etc/systemd/resolved.conf ]; then
			sed -i 's/^#\?DNSStubListener=.*/DNSStubListener=no/' /etc/systemd/resolved.conf 2>/dev/null || true
		fi
		systemctl restart systemd-resolved 2>/dev/null || true
		if [ -f /run/systemd/resolve/resolv.conf ]; then
			ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf
		fi
	fi

	for conflict_svc in dnsmasq bind9 named unbound; do
		if systemctl is-active --quiet "$conflict_svc" 2>/dev/null; then
			systemctl stop "$conflict_svc" 2>/dev/null || true
			systemctl disable "$conflict_svc" 2>/dev/null || true
		fi
	done
fi

# Function to write pristine default configuration
write_pristine_config() {
	cat << 'EOF' > "$INSTALL_DIR/AdGuardHome.yaml"
http:
  address: 0.0.0.0:80
  session_ttl: 720h
users:
  - name: admin
    password: "$2a$10$AUqri/85mab2pjf6u7uKSuVP7Uqtv3aDHq0yZMKOHElbCQ5J7AmQy"
dns:
  bind_hosts:
    - 0.0.0.0
  port: 53
  upstream_dns:
    - tls://dns.alidns.com
    - https://dns.alidns.com/dns-query
    - tls://one.one.one.one
    - https://cloudflare-dns.com/dns-query
    - tls://ordns.he.net
    - tls://dns11.quad9.net
    - tls://dot.pub
    - tls://adblock.dns.mullvad.net
    - https://dns11.quad9.net/dns-query
    - https://wikimedia-dns.org/dns-query
  bootstrap_dns:
    - 1.1.1.1
    - 8.8.8.8
    - 9.9.9.11
    - 223.5.5.5
    - 2606:4700:4700::1111
    - 2001:4860:4860::8888
  cache_size: 4194304
  cache_enabled: true
filtering:
  filtering_enabled: true
  protection_enabled: true
schema_version: 34
EOF
}

# Ensure AdGuardHome.yaml exists or create it
if [ ! -f "$INSTALL_DIR/AdGuardHome.yaml" ]; then
	echo "${BLUE}Membuat konfigurasi AdGuardHome.yaml default...${NC}"
	write_pristine_config
else
	# Ensure users section exists
	if ! grep -q "^users:" "$INSTALL_DIR/AdGuardHome.yaml"; then
		echo "${YELLOW}[!] Menambahkan akun admin default...${NC}"
		cat << 'EOF' >> "$INSTALL_DIR/AdGuardHome.yaml"

users:
  - name: admin
    password: "$2a$10$AUqri/85mab2pjf6u7uKSuVP7Uqtv3aDHq0yZMKOHElbCQ5J7AmQy"
EOF
	fi

	# Hotspot coexistence check
	if ip addr show 2>/dev/null | grep -q '10.42.0.1' || (command -v ss >/dev/null 2>&1 && ss -tulpn 2>/dev/null | grep -q '10.42.0.1:53'); then
		MAIN_IP="$(hostname -I 2>/dev/null | awk '{print $1}' || echo '')"
		if [ "$MAIN_IP" != '' ] && [ "$MAIN_IP" != '10.42.0.1' ]; then
			if grep -q -- "- 0.0.0.0" "$INSTALL_DIR/AdGuardHome.yaml"; then
				awk -v ip="$MAIN_IP" '{
					if ($0 ~ /^[[:space:]]*- 0\.0\.0\.0/) {
						print "    - 127.0.0.1";
						print "    - " ip;
					} else {
						print $0;
					}
				}' "$INSTALL_DIR/AdGuardHome.yaml" > "$INSTALL_DIR/AdGuardHome.yaml.tmp" && mv "$INSTALL_DIR/AdGuardHome.yaml.tmp" "$INSTALL_DIR/AdGuardHome.yaml"
			fi
		fi
	fi
fi

# Validate configuration integrity before launching service
if [ -f "$INSTALL_DIR/AdGuardHome" ] && [ -f "$INSTALL_DIR/AdGuardHome.yaml" ]; then
	echo "${BLUE}Memverifikasi integritas konfigurasi AdGuardHome.yaml...${NC}"
	if ! "$INSTALL_DIR/AdGuardHome" --check-config -c "$INSTALL_DIR/AdGuardHome.yaml" -w "$INSTALL_DIR" >/dev/null 2>&1; then
		echo "${YELLOW}[!] Format AdGuardHome.yaml tidak valid. Memulihkan konfigurasi bersih...${NC}"
		if [ -f "$INSTALL_DIR/AdGuardHome.yaml.bak" ] && "$INSTALL_DIR/AdGuardHome" --check-config -c "$INSTALL_DIR/AdGuardHome.yaml.bak" -w "$INSTALL_DIR" >/dev/null 2>&1; then
			cp "$INSTALL_DIR/AdGuardHome.yaml.bak" "$INSTALL_DIR/AdGuardHome.yaml"
			echo "${GREEN}[✓] Berhasil memulihkan konfigurasi dari backup.${NC}"
		else
			write_pristine_config
			echo "${GREEN}[✓] Konfigurasi bersih baru berhasil dipasang.${NC}"
		fi
	else
		echo "${GREEN}[✓] Konfigurasi valid.${NC}"
	fi
fi

# Ensure correct file permissions
chmod 755 "$INSTALL_DIR/AdGuardHome" 2>/dev/null || true
chmod 644 "$INSTALL_DIR/AdGuardHome.yaml" 2>/dev/null || true

# Register systemd service if not present
if [ ! -f /etc/systemd/system/AdGuardHome.service ] && [ -f "$INSTALL_DIR/AdGuardHome" ]; then
	echo "${BLUE}Mendaftarkan AdGuardHome sebagai service systemd...${NC}"
	(cd "$INSTALL_DIR" && ./AdGuardHome -s install >/dev/null 2>&1) || true
fi

# 7. Restart Service
echo "${BLUE}[5/5] Memulai kembali service DNS SERVER BRST...${NC}"
if command -v systemctl >/dev/null 2>&1; then
	systemctl daemon-reload 2>/dev/null || true
	systemctl restart AdGuardHome || true
	sleep 2

	if systemctl is-active --quiet AdGuardHome; then
		echo "${GREEN}[✓] Service AdGuardHome aktif dan berjalan normal.${NC}"
	else
		echo "${RED}[!] Peringatan: Service AdGuardHome gagal dijalankan otomatis. Menampilkan log...${NC}"
		journalctl -u AdGuardHome.service -n 15 --no-pager 2>/dev/null || true
	fi
else
	"$INSTALL_DIR/AdGuardHome" -s start
fi

IP_ADDR="$(hostname -I 2>/dev/null | awk '{print $1}' || echo "IP_SERVER")"

echo ""
echo "${GREEN}=====================================================${NC}"
echo "${GREEN}   [✓] UPDATE DNS SERVER BRST SELESAI & AKTIF!      ${NC}"
echo "${GREEN}=====================================================${NC}"
echo "Dashboard Admin dapat diakses di:"
echo "${CYAN}  http://${IP_ADDR}${NC}"
echo ""
echo "Login Administrator:"
echo "  Username: admin"
echo "  Password: admin (atau password yang telah Anda ubah)"
echo ""
echo "Perintah status:"
echo "  sudo systemctl status AdGuardHome"
echo "====================================================="
