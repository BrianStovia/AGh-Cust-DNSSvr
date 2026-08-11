#!/bin/sh

# DNS SERVER BRST / AdGuard Home Installation Script (Debian-based Systems)

# Exit the script if a pipeline fails (-e), prevent accidental filename
# expansion (-f), and consider undefined variables as errors (-u).
set -e -f -u

# Function log is an echo wrapper that writes to stderr if the caller
# requested verbosity level greater than 0. Otherwise, it does nothing.
log() {
	if [ "$verbose" -gt '0' ]; then
		echo "$1" 1>&2
	fi
}

# Function error_exit is an echo wrapper that writes to stderr and stops the
# script execution with code 1.
error_exit() {
	echo "$1" 1>&2

	exit 1
}

# Function usage prints the note about how to use the script.
usage() {
	echo 'install-debian.sh: usage: [-c channel] [-C cpu_type] [-h] [-O os] [-o output_dir]' \
		'[-r|-R] [-u|-U] [-v|-V]' 1>&2

	exit 2
}

# Function maybe_sudo runs passed command with root privileges if use_sudo isn't equal to 0.
maybe_sudo() {
	if [ "$use_sudo" -eq 0 ]; then
		"$@"
	else
		"$sudo_cmd" "$@"
	fi
}

# Function is_command checks if the command exists on the machine.
is_command() {
	command -v "$1" >/dev/null 2>&1
}

# Function is_little_endian checks if the CPU is little-endian.
is_little_endian() {
	is_little_endian_result="$(
		printf 'I' \
			| hexdump -o \
			| awk '{ print substr($2, 6, 1); exit; }'
	)"
	readonly is_little_endian_result

	[ "$is_little_endian_result" -eq '1' ]
}

# Function check_required checks if the required software is available on the machine.
check_required() {
	required="tar"
	readonly required

	for cmd in $required; do
		log "checking $cmd"
		if ! is_command "$cmd"; then
			log "installing $cmd via apt-get"
			maybe_sudo apt-get update -qq && maybe_sudo apt-get install -y -qq "$cmd"
		fi
	done
}

# Function check_out_dir requires the output directory to be set and exist.
check_out_dir() {
	if [ "$out_dir" = '' ]; then
		error_exit 'output directory should be presented'
	fi

	if ! [ -d "$out_dir" ]; then
		log "$out_dir directory will be created"
	fi
}

# Function parse_opts parses the options list and validates its combinations.
parse_opts() {
	while getopts "C:c:hO:o:rRuUvV" opt "$@"; do
		case "$opt" in
		C)
			cpu="$OPTARG"
			;;
		c)
			channel="$OPTARG"
			;;
		h)
			usage
			;;
		O)
			os="$OPTARG"
			;;
		o)
			out_dir="$OPTARG"
			;;
		R)
			reinstall='0'
			;;
		U)
			uninstall='0'
			;;
		r)
			reinstall='1'
			;;
		u)
			uninstall='1'
			;;
		V)
			verbose='0'
			;;
		v)
			verbose='1'
			;;
		*)
			log "bad option $OPTARG"
			usage
			;;
		esac
	done

	if [ "$uninstall" -eq '1' ] && [ "$reinstall" -eq '1' ]; then
		error_exit 'the -r and -u options are mutually exclusive'
	fi
}

# Function set_channel sets the channel if needed and validates the value.
set_channel() {
	case "$channel" in
	'development' | 'edge' | 'beta' | 'release')
		;;
	*)
		error_exit "invalid channel '$channel'
supported values are 'development', 'edge', 'beta', and 'release'"
		;;
	esac

	log "channel: $channel"
}

# Function set_os sets the os if needed and validates Debian compatibility.
set_os() {
	if [ "$os" = '' ]; then
		os="$(uname -s)"
		case "$os" in
		'Linux')
			os='linux'
			;;
		*)
			error_exit "unsupported operating system for install-debian.sh: '$os'"
			;;
		esac
	fi

	if [ -f /etc/os-release ]; then
		. /etc/os-release
		log "detected OS: ${NAME:-Debian}"
	fi

	log "operating system: $os"
}

# Function set_cpu sets the cpu if needed and validates the value.
set_cpu() {
	if [ "$cpu" = '' ]; then
		cpu="$(uname -m)"
		case "$cpu" in
		'x86_64' | 'x86-64' | 'x64' | 'amd64')
			cpu='amd64'
			;;
		'i386' | 'i486' | 'i686' | 'i786' | 'x86')
			cpu='386'
			;;
		'armv5l')
			cpu='armv5'
			;;
		'armv6l')
			cpu='armv6'
			;;
		'armv7l' | 'armv8l')
			cpu='armv7'
			;;
		'aarch64' | 'arm64')
			cpu='arm64'
			;;
		'mips' | 'mips64')
			if is_little_endian; then
				cpu="${cpu}le"
			fi
			cpu="${cpu}_softfloat"
			;;
		'riscv64')
			cpu='riscv64'
			;;
		*)
			error_exit "unsupported cpu type: $cpu"
			;;
		esac
	fi

	case "$cpu" in
	'amd64' | '386' | 'armv5' | 'armv6' | 'armv7' | 'arm64' | 'riscv64')
		;;
	'mips64le_softfloat' | 'mips64_softfloat' | 'mipsle_softfloat' | 'mips_softfloat')
		;;
	*)
		error_exit "unsupported cpu type: $cpu"
		;;
	esac

	log "cpu type: $cpu"
}

# Function download_curl uses curl(1) to download a file.
download_curl() {
	curl_output="${2:-}"
	if [ "$curl_output" = '' ]; then
		curl -L -S -s "$1"
	else
		curl -L -S -o "$curl_output" -s "$1"
	fi
}

# Function download_wget uses wget(1) to download a file.
download_wget() {
	wget_output="${2:--}"
	wget --no-verbose -O "$wget_output" "$1"
}

# Function download_fetch uses fetch(1) to download a file.
download_fetch() {
	fetch_output="${2:-}"
	if [ "$fetch_output" = '' ]; then
		fetch -o '-' "$1"
	else
		fetch -o "$fetch_output" "$1"
	fi
}

# Function set_download_func sets the appropriate function for downloading files.
set_download_func() {
	if is_command 'curl'; then
		return 0
	elif is_command 'wget'; then
		download_func='download_wget'
	elif is_command 'fetch'; then
		download_func='download_fetch'
	else
		error_exit "either curl or wget is required to install DNS SERVER BRST via this script"
	fi
}

# Function set_sudo_cmd sets the appropriate command to run a command under superuser privileges.
set_sudo_cmd() {
	sudo_cmd='sudo'
}

# Function apply_debian_optimizations tunes kernel sysctl parameters for Debian hosts.
apply_debian_optimizations() {
	log 'applying Debian network sysctl performance optimizations'
	sysctl_conf="/etc/sysctl.d/99-dns-server-brst.conf"

	maybe_sudo sh -c "cat << 'EOF' > '$sysctl_conf'
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
EOF"

	if is_command 'sysctl'; then
		maybe_sudo sysctl -p "$sysctl_conf" >/dev/null 2>&1 || maybe_sudo sysctl --system >/dev/null 2>&1
	fi
}

# Function configure sets the script's configuration.
configure() {
	set_channel
	set_os
	set_cpu
	set_download_func
	set_sudo_cmd
	check_out_dir

	pkg_name="AdGuardHome_${os}_${cpu}"
	url="https://raw.githubusercontent.com/BrianStovia/AGh-Cust-DNSSvr/main/dist/${pkg_name}"
	agh_dir="${out_dir}/AdGuardHome"
	readonly pkg_name url agh_dir

	log "DNS SERVER BRST / AdGuard Home will be installed into $agh_dir"
}

# Function is_root checks for root privileges to be granted.
is_root() {
	user_id="$(id -u)"
	if [ "$user_id" -eq '0' ]; then
		log 'script is executed with root privileges'
		return 0
	fi

	if is_command "$sudo_cmd"; then
		log 'note that DNS SERVER BRST requires root privileges to install using this script'
		return 1
	fi

	error_exit 'root privileges are required to install DNS SERVER BRST using this script
please, restart it with root privileges'
}

# Function rerun_with_root downloads the script, runs it with root privileges,
# and exits the current script.
rerun_with_root() {
	r='-R'
	if [ "$reinstall" -eq '1' ]; then
		r='-r'
	fi

	u='-U'
	if [ "$uninstall" -eq '1' ]; then
		u='-u'
	fi

	v='-V'
	if [ "$verbose" -eq '1' ]; then
		v='-v'
	fi

	readonly r u v

	log 'restarting with root privileges'

	if [ -f "$0" ]; then
		$sudo_cmd sh "$0" -c "$channel" -C "$cpu" -O "$os" -o "$out_dir" "$r" "$u" "$v"
	else
		script_url='https://raw.githubusercontent.com/BrianStovia/AGh-Cust-DNSSvr/main/scripts/install-debian.sh'
		{ "$download_func" "$script_url" || echo 'exit 1'; } \
			| $sudo_cmd sh -s -- -c "$channel" -C "$cpu" -O "$os" -o "$out_dir" "$r" "$u" "$v"
	fi

	exit 0
}

# Function download downloads the file from the URL or skips if local binary is found.
download() {
	if [ -f "./AdGuardHome" ] || [ -f "../AdGuardHome" ]; then
		log "custom local binary detected, skipping remote download"
		return 0
	fi

	log "downloading custom binary from $url to $pkg_name"

	SUCCESS=0
	if "$download_func" "$url" "$pkg_name" 2>/dev/null && [ -s "$pkg_name" ]; then
		if head -c 4 "$pkg_name" 2>/dev/null | grep -q 'ELF'; then
			SUCCESS=1
		fi
	fi

	if [ "$SUCCESS" -eq 0 ]; then
		error_exit "cannot download custom DNS SERVER BRST binary from $url into $pkg_name"
	fi

	log "successfully prepared binary"
}

# Function unpack unpacks or copies the downloaded custom binary into destination.
unpack() {
	# shellcheck disable=SC2174
	if ! mkdir -m 0700 -p "$out_dir"; then
		error_exit "cannot create directory $out_dir"
	fi

	mkdir -p "$agh_dir"

	if [ -f "./AdGuardHome" ]; then
		log "copying custom binary ./AdGuardHome into $agh_dir"
		cp "./AdGuardHome" "$agh_dir/AdGuardHome"
		chmod +x "$agh_dir/AdGuardHome"
		return 0
	elif [ -f "../AdGuardHome" ]; then
		log "copying custom binary ../AdGuardHome into $agh_dir"
		cp "../AdGuardHome" "$agh_dir/AdGuardHome"
		chmod +x "$agh_dir/AdGuardHome"
		return 0
	elif [ -f "$pkg_name" ]; then
		log "installing downloaded custom binary $pkg_name into $agh_dir"
		cp "$pkg_name" "$agh_dir/AdGuardHome"
		chmod +x "$agh_dir/AdGuardHome"
		rm -f "$pkg_name"
		return 0
	fi

	error_exit "failed to locate or install custom binary"
}

# Function handle_existing detects existing installation and takes care of removing it if needed.
handle_existing() {
	if ! [ -d "$agh_dir" ]; then
		log 'no need to uninstall'

		if [ "$uninstall" -eq '1' ]; then
			exit 0
		fi

		return 0
	fi

	existing_adguard_home="$(ls -1 -A "$agh_dir")"
	if [ "$existing_adguard_home" != '' ]; then
		log 'the existing DNS SERVER BRST / AdGuard Home installation is detected'

		if [ "$reinstall" -ne '1' ] && [ "$uninstall" -ne '1' ]; then
			error_exit \
				"to reinstall/uninstall the DNS SERVER BRST using this script specify one of the '-r' or '-u' flags"
		fi

		if (cd "$agh_dir" && ! ./AdGuardHome -s stop || ! ./AdGuardHome -s uninstall); then
			log "cannot uninstall DNS SERVER BRST from $agh_dir"
		fi

		rm -r "$agh_dir"

		log 'DNS SERVER BRST was successfully uninstalled'
	fi

	if [ "$uninstall" -eq '1' ]; then
		exit 0
	fi
}

# Function install_service tries to install DNS SERVER BRST as a service.
install_service() {
	use_sudo='0'

	if (cd "$agh_dir" && maybe_sudo ./AdGuardHome -s install); then
		return 0
	fi

	log "installation failed, removing $agh_dir"
	rm -r "$agh_dir"

	error_exit 'cannot install DNS SERVER BRST as a service'
}

# Entrypoint

# Set default values of configuration variables.
channel='release'
reinstall='1'
uninstall='0'
verbose='0'
cpu=''
os=''
out_dir='/opt'
pkg_ext='tar.gz'
download_func='download_curl'
sudo_cmd='sudo'

parse_opts "$@"

echo 'starting DNS SERVER BRST installation script'

configure
check_required

if ! is_root; then
	rerun_with_root
fi

handle_existing
apply_debian_optimizations

download
unpack

install_service

printf '%s\n' \
	'DNS SERVER BRST is now installed and running' \
	'you can control the service status with the following commands:' \
	"$sudo_cmd ${agh_dir}/AdGuardHome -s start|stop|restart|status|install|uninstall"
