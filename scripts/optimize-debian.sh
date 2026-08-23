#!/bin/sh

# AdGuard Home Debian/Ubuntu Performance Optimization Script
#
# This script applies kernel sysctl tuning for low-latency, high-throughput
# DNS query processing on Debian-based Linux systems.

set -e -u

SYSCTL_CONF="/etc/sysctl.d/99-adguardhome-performance.conf"

echo "Applying Linux Kernel optimizations for AdGuard Home..."

if [ "$(id -u)" -ne 0 ]; then
	echo "Error: This script must be run as root or via sudo." 1>&2
	exit 1
fi

cat << 'EOF' > "$SYSCTL_CONF"
# AdGuard Home Performance Optimizations

# Maximum socket receive/send buffer sizes (8MB)
net.core.rmem_max = 8388608
net.core.wmem_max = 8388608
net.core.rmem_default = 1048576
net.core.wmem_default = 1048576

# UDP buffer sizes
net.ipv4.udp_rmem_min = 16384
net.ipv4.udp_wmem_min = 16384

# Maximum network backlog queue
net.core.netdev_max_backlog = 10000

# Maximum socket connection backlog
net.core.somaxconn = 4096

# Ephemeral port range for DNS upstream queries
net.ipv4.ip_local_port_range = 1024 65535

# Enable TCP Fast Open where available
net.ipv4.tcp_fastopen = 3

# Decrease FIN timeout for quick socket recycling
net.ipv4.tcp_fin_timeout = 15

EOF

echo "Written configuration to $SYSCTL_CONF"

# Disable systemd-resolved stub listener if active to release port 53
if command -v systemctl >/dev/null 2>&1 && systemctl is-enabled systemd-resolved >/dev/null 2>&1; then
	echo "Releasing port 53 by disabling systemd-resolved DNSStubListener..."
	mkdir -p /etc/systemd/resolved.conf.d
	cat << 'EOF' > /etc/systemd/resolved.conf.d/adguardhome.conf
[Resolve]
DNS=127.0.0.1
DNSStubListener=no
EOF
	if [ -f /run/systemd/resolve/resolv.conf ]; then
		ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf
	fi
	systemctl restart systemd-resolved 2>/dev/null || true
	echo "systemd-resolved stub disabled and port 53 released."
fi

echo "Debian network optimizations for AdGuard Home complete!"
