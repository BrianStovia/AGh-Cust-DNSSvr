#!/bin/sh

# ==============================================================================
# Auto-Updater for DNS SERVER BRST on Debian-based Linux Systems
# (Debian, Ubuntu, Raspberry Pi OS, Pop!_OS, Linux Mint)
# ==============================================================================

set -e -u

RED='\033[0;31m'
GREEN='\033[0;32m'
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
	echo "${RED}Error: Direktori instalasi $INSTALL_DIR tidak ditemukan.${NC}" 1>&2
	echo "Jalankan installer terlebih dahulu: sudo sh scripts/install-debian.sh"
	exit 1
fi

# 3. Backup Configuration
echo "${BLUE}[1/5] Membackup konfigurasi lama ke $INSTALL_DIR/AdGuardHome.yaml.bak...${NC}"
if [ -f "$INSTALL_DIR/AdGuardHome.yaml" ]; then
	cp "$INSTALL_DIR/AdGuardHome.yaml" "$INSTALL_DIR/AdGuardHome.yaml.bak"
	echo "${GREEN}[✓] Konfigurasi berhasil dibackup.${NC}"
fi

# 4. Stop Service
echo "${BLUE}[2/5] Menghentikan service DNS SERVER BRST...${NC}"
if command -v systemctl >/dev/null 2>&1; then
	systemctl stop AdGuardHome >/dev/null 2>&1 || true
else
	"$INSTALL_DIR/AdGuardHome" -s stop >/dev/null 2>&1 || true
fi

# 5. Update Binary
echo "${BLUE}[3/5] Memperbarui binary DNS SERVER BRST...${NC}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$SCRIPT_DIR/../AdGuardHome" ]; then
	cp "$SCRIPT_DIR/../AdGuardHome" "$INSTALL_DIR/AdGuardHome"
	chmod +x "$INSTALL_DIR/AdGuardHome"
	echo "${GREEN}[✓] Binary kustom lokal berhasil dipasang.${NC}"
elif [ -f "./AdGuardHome" ]; then
	cp "./AdGuardHome" "$INSTALL_DIR/AdGuardHome"
	chmod +x "$INSTALL_DIR/AdGuardHome"
	echo "${GREEN}[✓] Binary kustom lokal berhasil dipasang.${NC}"
else
	echo "${BLUE}Mengunduh paket biner DNS SERVER BRST kustom terbaru dari GitHub...${NC}"
	ARCH="$(uname -m)"
	case "$ARCH" in
	x86_64) ARCH_TYPE="amd64" ;;
	aarch64 | arm64) ARCH_TYPE="arm64" ;;
	*) ARCH_TYPE="amd64" ;;
	esac

	DOWNLOAD_URL="https://raw.githubusercontent.com/BrianStovia/AGh-Cust-DNSSvr/main/dist/AdGuardHome_linux_${ARCH_TYPE}"
	TMP_BIN="/tmp/AdGuardHome_custom_update"
	curl -sSL "$DOWNLOAD_URL" -o "$TMP_BIN"
	cp "$TMP_BIN" "$INSTALL_DIR/AdGuardHome"
	chmod +x "$INSTALL_DIR/AdGuardHome"
	rm -f "$TMP_BIN"
	echo "${GREEN}[✓] Binary kustom terbaru berhasil dipasang.${NC}"
fi

# 6. Re-apply Debian Kernel Performance Tuning
echo "${BLUE}[4/5] Memeriksa dan menguji ulang optimasi kernel sysctl...${NC}"
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
	sysctl -p "$SYSCTL_CONF" >/dev/null 2>&1 || sysctl --system >/dev/null 2>&1
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

# Ensure AdGuardHome has users configured (prevents accidental userless/no-login state)
if [ -f "$INSTALL_DIR/AdGuardHome.yaml" ]; then
	if ! grep -q "^users:" "$INSTALL_DIR/AdGuardHome.yaml"; then
		echo "${YELLOW}[!] Menambahkan akun admin default karena bagian users belum dikonfigurasi...${NC}"
		if grep -q "^schema_version:" "$INSTALL_DIR/AdGuardHome.yaml"; then
			sed -i '/^schema_version:/i users:\n  - name: admin\n    password: "$2a$10$AUqri/85mab2pjf6u7uKSuVP7Uqtv3aDHq0yZMKOHElbCQ5J7AmQy"' "$INSTALL_DIR/AdGuardHome.yaml"
		else
			cat << 'EOF' >> "$INSTALL_DIR/AdGuardHome.yaml"
users:
  - name: admin
    password: "$2a$10$AUqri/85mab2pjf6u7uKSuVP7Uqtv3aDHq0yZMKOHElbCQ5J7AmQy"
EOF
		fi
	fi
fi

# Ensure AdGuardHome can coexist with Hotspot if 10.42.0.1 is active and AdGuardHome.yaml has 0.0.0.0
if [ -f "$INSTALL_DIR/AdGuardHome.yaml" ]; then
	if ip addr show 2>/dev/null | grep -q '10.42.0.1' || (command -v ss >/dev/null 2>&1 && ss -tulpn 2>/dev/null | grep -q '10.42.0.1:53'); then
		MAIN_IP="$(hostname -I 2>/dev/null | awk '{print $1}' || echo '')"
		if [ "$MAIN_IP" != '' ] && [ "$MAIN_IP" != '10.42.0.1' ]; then
			if grep -q -- "- 0.0.0.0" "$INSTALL_DIR/AdGuardHome.yaml"; then
				sed -i "/- 0.0.0.0/c\    - 127.0.0.1\n    - ${MAIN_IP}" "$INSTALL_DIR/AdGuardHome.yaml"
			fi
		fi
	fi
fi

# 7. Restart Service
echo "${BLUE}[5/5] Memulai kembali service DNS SERVER BRST...${NC}"
if command -v systemctl >/dev/null 2>&1; then
	systemctl restart AdGuardHome
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
