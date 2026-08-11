#!/bin/sh

# ==============================================================================
# Uninstaller Script for DNS SERVER BRST / AdGuard Home on Debian-based Linux
# (Debian, Ubuntu, Raspberry Pi OS, Pop!_OS, Linux Mint)
# ==============================================================================

set -e -u

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "${CYAN}=====================================================${NC}"
echo "${CYAN}     DNS SERVER BRST - Uninstaller (Debian)          ${NC}"
echo "${CYAN}=====================================================${NC}"
echo ""

# 1. Root privilege check
if [ "$(id -u)" -ne 0 ]; then
	echo "${RED}Error: Skrip uninstaller ini harus dijalankan dengan akses root (sudo).${NC}" 1>&2
	exit 1
fi

INSTALL_DIR="/opt/AdGuardHome"

# 2. Stop & Uninstall Service
echo "${BLUE}[1/4] Menghentikan & mencopot service DNS SERVER BRST...${NC}"

if [ -f "$INSTALL_DIR/AdGuardHome" ]; then
	"$INSTALL_DIR/AdGuardHome" -s uninstall 2>/dev/null || true
fi

if command -v systemctl >/dev/null 2>&1; then
	systemctl stop AdGuardHome 2>/dev/null || true
	systemctl disable AdGuardHome 2>/dev/null || true
	rm -f /etc/systemd/system/AdGuardHome.service
	systemctl daemon-reload 2>/dev/null || true
fi

echo "${GREEN}[✓] Service berhasil dihentikan & dicopot.${NC}"

# 3. Restore systemd-resolved DNSStubListener if modified
echo "${BLUE}[2/4] Mengembalikan pengaturan DNS bawaan sistem...${NC}"
if [ -f /etc/systemd/resolved.conf ]; then
	sed -i 's/DNSStubListener=no/#DNSStubListener=yes/' /etc/systemd/resolved.conf 2>/dev/null || true
	systemctl restart systemd-resolved 2>/dev/null || true
fi

# 4. Remove Kernel sysctl optimizations
echo "${BLUE}[3/4] Menghapus file konfigurasi kernel sysctl BRST...${NC}"
rm -f /etc/sysctl.d/99-dns-server-brst.conf
if command -v sysctl >/dev/null 2>&1; then
	sysctl --system >/dev/null 2>&1 || true
fi

# 5. Remove installation directory
echo "${BLUE}[4/4] Menghapus berkas instalasi di $INSTALL_DIR...${NC}"
rm -rf "$INSTALL_DIR"

echo ""
echo "${GREEN}=====================================================${NC}"
echo "${GREEN} [✓] UNINSTALL DNS SERVER BRST SELESAI!               ${NC}"
echo "${GREEN}=====================================================${NC}"
echo "DNS SERVER BRST dan seluruh berkasnya telah berhasil dicopot dari sistem."
echo "====================================================="
