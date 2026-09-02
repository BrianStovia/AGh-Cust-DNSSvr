import { type JSX, createSignal, createMemo, Show, For } from 'solid-js';
import { A } from '@solidjs/router';
import intl from 'panel/common/intl';
import cn from 'clsx';

import { encryptionState } from 'panel/stores/encryption';
import { MOBILE_CONFIG_LINKS } from 'panel/helpers/constants';
import { MobileConfigForm } from 'panel/components/SetupGuide/MobileConfigForm';
import { Paths } from 'panel/components/Routes/Paths';
import { type IconType } from '../Icons';
import { Icon } from '../Icon';
import { CopiedText } from '../CopiedText';
import s from './Guide.module.pcss';

type PlatformLayoutProps = {
    serverName?: string;
    portHttps?: number;
    dnsAddresses?: string[];
};

type PlatformLayout = {
    title: string;
    icon: IconType;
    component: JSX.Element;
};

type PlatformLayouts = Record<string, PlatformLayout>;

const RouterLayout = (props: PlatformLayoutProps) => {
    const plainIps = () => props.dnsAddresses?.filter((a) => !a.includes('://')) ?? [];

    return (
        <div class={s.platformContainer}>
            <div class={s.title}>{intl.getMessage('setup_devices_router_title')}</div>

            <div class={s.methodCard}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadge}>Seluruh Jaringan Rumah</span>
                    <h3 class={s.methodTitle}>Konfigurasi DNS pada Router Wi-Fi</h3>
                </div>
                <p class={s.methodDesc}>
                    Dengan mengatur DNS di router, semua perangkat di rumah (HP, PC, Smart TV, konsol) otomatis terlindungi tanpa perlu setelan manual di tiap perangkat.
                </p>
                <ol class={s.stepList}>
                    <li>
                        Buka browser dan akses halaman admin router Anda (biasanya{' '}
                        <span class={s.inlineCopiedText}>
                            <CopiedText text="192.168.1.1" />
                        </span>{' '}
                        atau{' '}
                        <span class={s.inlineCopiedText}>
                            <CopiedText text="192.168.0.1" />
                        </span>
                        ).
                    </li>
                    <li>Cari menu <strong>WAN</strong>, <strong>Internet</strong>, atau <strong>DHCP Server</strong>.</li>
                    <li>Ganti alamat DNS 1 dan DNS 2 dengan IP server AdGuard Home ini:</li>
                </ol>

                <Show when={plainIps().length > 0} fallback={<p class={s.methodDesc}>Tidak ada alamat IP lokal terdeteksi.</p>}>
                    <div class={s.ipGrid}>
                        <For each={plainIps()}>
                            {(ip) => (
                                <div class={s.ipPill}>
                                    <CopiedText text={ip} />
                                </div>
                            )}
                        </For>
                    </div>
                </Show>

                <p class={s.methodDesc} style={{ 'margin-top': '16px' }}>
                    Ingin AdGuard Home menangani DHCP secara langsung?{' '}
                    <A href={Paths.Dhcp} class={s.dhcpLink}>
                        Buka Pengaturan DHCP Server
                    </A>
                </p>
            </div>
        </div>
    );
};

const WindowsLayout = (props: PlatformLayoutProps) => {
    const plainIps = () => props.dnsAddresses?.filter((a) => !a.includes('://')) ?? [];
    const httpsUrl = () => props.dnsAddresses?.find((a) => a.startsWith('https://'));

    return (
        <div class={s.platformContainer}>
            <div class={s.title}>{intl.getMessage('setup_devices_windows_title')}</div>

            <Show when={httpsUrl()}>
                <div class={s.methodCard}>
                    <div class={s.methodHeader}>
                        <span class={s.methodBadge}>Windows 11 (DoH)</span>
                        <h3 class={s.methodTitle}>DNS-over-HTTPS Bawaan</h3>
                    </div>
                    <p class={s.methodDesc}>
                        Windows 11 mendukung enkripsi DoH secara langsung dari menu Pengaturan Jaringan:
                    </p>
                    <ol class={s.stepList}>
                        <li>Buka <strong>Settings</strong> &gt; <strong>Network &amp; internet</strong> &gt; <strong>Wi-Fi</strong> atau <strong>Ethernet</strong>.</li>
                        <li>Klik <strong>Hardware properties</strong> &gt; tombol Edit pada <strong>DNS server assignment</strong> &gt; pilih <strong>Manual</strong>.</li>
                        <li>Aktifkan <strong>IPv4</strong>, masukkan IP server, dan pilih <strong>Encrypted only (DNS over HTTPS)</strong>.</li>
                        <li>Masukkan URL DoH berikut:</li>
                    </ol>
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>DoH Template URL:</span>
                        <div class={s.copyField}>
                            <CopiedText text={httpsUrl()!} />
                        </div>
                    </div>
                </div>
            </Show>

            <div class={s.methodCard}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadgeSecondary}>Standar / Windows 10</span>
                    <h3 class={s.methodTitle}>Konfigurasi DNS IPv4 Manual</h3>
                </div>
                <p class={s.methodDesc}>
                    Buka Control Panel &gt; Network Connections &gt; Properties &gt; Internet Protocol Version 4 (TCP/IPv4), lalu masukkan IP berikut:
                </p>
                <div class={s.ipGrid}>
                    <For each={plainIps()}>
                        {(ip) => (
                            <div class={s.ipPill}>
                                <CopiedText text={ip} />
                            </div>
                        )}
                    </For>
                </div>
            </div>
        </div>
    );
};

const MacOSLayout = (props: PlatformLayoutProps) => {
    const plainIps = () => props.dnsAddresses?.filter((a) => !a.includes('://')) ?? [];

    return (
        <div class={s.platformContainer}>
            <div class={s.title}>macOS</div>

            <div class={s.methodCard}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadge}>macOS Big Sur+</span>
                    <h3 class={s.methodTitle}>Profil Konfigurasi Enkripsi Apple (.mobileconfig)</h3>
                </div>
                <p class={s.methodDesc}>
                    Unduh dan pasang profil konfigurasi untuk mengamankan seluruh kueri DNS di macOS menggunakan DNS-over-HTTPS:
                </p>
                <div class={s.mobileConfigContainer}>
                    <MobileConfigForm
                        initialValues={{
                            host: props.serverName || '',
                            clientId: '',
                            protocol: MOBILE_CONFIG_LINKS.DOH,
                            port: props.portHttps,
                        }}
                    />
                </div>
            </div>

            <Show when={plainIps().length > 0}>
                <div class={s.methodCard}>
                    <div class={s.methodHeader}>
                        <span class={s.methodBadgeSecondary}>Manual</span>
                        <h3 class={s.methodTitle}>Konfigurasi DNS Biasa (System Settings)</h3>
                    </div>
                    <ol class={s.stepList}>
                        <li>Buka <strong>System Settings</strong> &gt; <strong>Network</strong> &gt; Wi-Fi aktif &gt; <strong>Details</strong> &gt; <strong>DNS</strong>.</li>
                        <li>Tambahkan alamat IP server di bawah ini:</li>
                    </ol>
                    <div class={s.ipGrid}>
                        <For each={plainIps()}>
                            {(ip) => (
                                <div class={s.ipPill}>
                                    <CopiedText text={ip} />
                                </div>
                            )}
                        </For>
                    </div>
                </div>
            </Show>
        </div>
    );
};

const AndroidLayout = (props: PlatformLayoutProps) => {
    const cleanHostname = () =>
        props.serverName ||
        props.dnsAddresses
            ?.find((a) => a.startsWith('tls://'))
            ?.replace(/^tls:\/\//, '')
            .split(':')[0] ||
        '';

    const httpsUrl = () =>
        props.dnsAddresses?.find((a) => a.startsWith('https://')) ||
        (cleanHostname() ? `https://${cleanHostname()}/dns-query` : '');

    const plainIps = () => props.dnsAddresses?.filter((a) => !a.includes('://')) ?? [];

    return (
        <div class={s.platformContainer}>
            <div class={s.title}>{intl.getMessage('setup_devices_android_title')}</div>

            {/* Method 1: Private DNS (Android 9+) */}
            <div class={s.methodCard}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadge}>Rekomendasi (Android 9+)</span>
                    <h3 class={s.methodTitle}>Private DNS (DNS Pribadi)</h3>
                </div>
                <p class={s.methodDesc}>
                    Metode resmi tercepat dan paling aman menggunakan DNS-over-TLS (DoT). Melindungi saat memakai Wi-Fi maupun Paket Data.
                </p>
                <ol class={s.stepList}>
                    <li>Buka <strong>Pengaturan (Settings)</strong> HP Android Anda.</li>
                    <li>Masuk ke <strong>Jaringan &amp; Internet (Network &amp; internet)</strong> &gt; <strong>DNS Pribadi (Private DNS)</strong>.</li>
                    <li>Pilih <strong>Nama host penyedia DNS pribadi (Private DNS provider hostname)</strong>.</li>
                    <li>Salin dan tempel hostname di bawah ini:</li>
                </ol>

                <Show
                    when={cleanHostname()}
                    fallback={
                        <div class={s.warningBox}>
                            Enkripsi TLS belum diaktifkan di server AdGuard Home. Silakan buka menu <strong>Pengaturan &gt; Pengaturan Enkripsi</strong> terlebih dahulu.
                        </div>
                    }
                >
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>Private DNS Hostname:</span>
                        <div class={s.copyField}>
                            <CopiedText text={cleanHostname()} />
                        </div>
                    </div>
                    <div class={s.tipNotice}>
                        <strong>⚠️ Penting:</strong> Masukkan <u>HANYA</u> nama domain di atas. <strong>Jangan</strong> menyertakan <code>tls://</code> atau port <code>:853</code>, karena Android akan error <em>&quot;Tidak dapat tersambung&quot;</em>.
                    </div>
                </Show>
            </div>

            {/* Method 2: DNS over HTTPS via App */}
            <Show when={httpsUrl()}>
                <div class={s.methodCard}>
                    <div class={s.methodHeader}>
                        <span class={s.methodBadgeSecondary}>DoH (Port 443)</span>
                        <h3 class={s.methodTitle}>DNS-over-HTTPS (via Aplikasi)</h3>
                    </div>
                    <p class={s.methodDesc}>
                        Gunakan metode ini jika port 853 diblokir oleh operator seluler Anda, atau jika ingin enkripsi via HTTPS:
                    </p>
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>DoH URL:</span>
                        <div class={s.copyField}>
                            <CopiedText text={httpsUrl()} />
                        </div>
                    </div>
                    <p class={s.methodDesc} style={{ 'margin-top': '8px' }}>
                        Dapat dipasang di aplikasi Android seperti <strong>Intra</strong>, <strong>Nebulo</strong>, atau <strong>AdGuard for Android</strong>.
                    </p>
                </div>
            </Show>

            {/* Method 3: Wi-Fi Manual Settings (Plain DNS) */}
            <Show when={plainIps().length > 0}>
                <div class={s.methodCard}>
                    <div class={s.methodHeader}>
                        <span class={s.methodBadgeSecondary}>Wi-Fi Manual</span>
                        <h3 class={s.methodTitle}>Pengaturan IP Statis (Android Jadul)</h3>
                    </div>
                    <p class={s.methodDesc}>
                        Jika perangkat belum mendukung Private DNS, Anda bisa mengatur DNS Wi-Fi secara manual:
                    </p>
                    <ol class={s.stepList}>
                        <li>Buka setelan Wi-Fi &gt; Ketuk nama Wi-Fi aktif &gt; Opsi Lanjutan.</li>
                        <li>Ubah Setelan IP ke <strong>Statis (Static)</strong>, lalu isi DNS 1 &amp; DNS 2:</li>
                    </ol>
                    <div class={s.ipGrid}>
                        <For each={plainIps()}>
                            {(ip) => (
                                <div class={s.ipPill}>
                                    <CopiedText text={ip} />
                                </div>
                            )}
                        </For>
                    </div>
                </div>
            </Show>
        </div>
    );
};

const IOSLayout = (props: PlatformLayoutProps) => {
    const httpsUrl = () => props.dnsAddresses?.find((a) => a.startsWith('https://'));

    return (
        <div class={s.platformContainer}>
            <div class={s.title}>{intl.getMessage('setup_devices_ios_title')}</div>

            <div class={s.methodCard}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadge}>Rekomendasi (iOS 14+)</span>
                    <h3 class={s.methodTitle}>Profil Konfigurasi Apple (.mobileconfig)</h3>
                </div>
                <p class={s.methodDesc}>
                    Pasang profil enkripsi DNS resmi Apple untuk mengamankan koneksi di Wi-Fi dan data seluler secara otomatis tanpa aplikasi tambahan:
                </p>
                <div class={s.mobileConfigContainer}>
                    <MobileConfigForm
                        initialValues={{
                            host: props.serverName || '',
                            clientId: '',
                            protocol: MOBILE_CONFIG_LINKS.DOH,
                            port: props.portHttps,
                        }}
                    />
                </div>
            </div>

            <Show when={httpsUrl()}>
                <div class={s.methodCard}>
                    <div class={s.methodHeader}>
                        <span class={s.methodBadgeSecondary}>DoH URL</span>
                        <h3 class={s.methodTitle}>DNS-over-HTTPS untuk Safari / Aplikasi</h3>
                    </div>
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>DoH URL:</span>
                        <div class={s.copyField}>
                            <CopiedText text={httpsUrl()!} />
                        </div>
                    </div>
                </div>
            </Show>
        </div>
    );
};

const renderDnsDevicesList = () => (
    <div class={s.deviceDnsList}>
        <div class={s.guideParagraph}>
            <div class={s.guideTitle}>
                <strong>Android</strong>
            </div>
            <ul class={s.guideList}>
                <li class={s.guideBulletItem}>
                    {intl.getMessage('setup_devices_dns_android_list_1')}
                </li>
                <li class={s.guideBulletItem}>
                    {intl.getMessage('setup_devices_dns_android_list_2', {
                        a: (text: string) => (
                            <a
                                href="https://link.adtidy.org/forward.html?action=android&from=ui&app=home"
                                target="_blank"
                                class={s.dnsLink}
                                rel="noopener noreferrer"
                            >
                                {text}
                            </a>
                        ),
                    })}
                </li>
                <li class={s.guideBulletItem}>
                    {intl.getMessage('setup_devices_dns_android_list_3', {
                        a: (text: string) => (
                            <a
                                href="https://getintra.org/"
                                target="_blank"
                                class={s.dnsLink}
                                rel="noopener noreferrer"
                            >
                                {text}
                            </a>
                        ),
                    })}
                </li>
            </ul>
        </div>

        <div class={s.guideParagraph}>
            <div class={s.guideTitle}>
                <strong>iOS</strong>
            </div>
            <ul class={s.guideList}>
                <li class={s.guideBulletItem}>
                    {intl.getMessage('setup_devices_dns_ios_list_1', {
                        a: (text: string) => (
                            <a
                                href="https://link.adtidy.org/forward.html?action=ios&from=ui&app=home"
                                target="_blank"
                                class={s.dnsLink}
                                rel="noopener noreferrer"
                            >
                                {text}
                            </a>
                        ),
                    })}
                </li>
                <li class={s.guideBulletItem}>
                    {intl.getMessage('setup_devices_dns_ios_list_2', {
                        a: (text: string) => (
                            <a
                                href="https://itunes.apple.com/app/id1452162351"
                                target="_blank"
                                class={s.dnsLink}
                                rel="noopener noreferrer"
                            >
                                {text}
                            </a>
                        ),
                        b: (text: string) => (
                            <a
                                href="https://dnscrypt.info/stamps"
                                target="_blank"
                                rel="noopener noreferrer"
                                class={s.dnsLink}
                            >
                                {text}
                            </a>
                        ),
                    })}
                </li>
            </ul>
        </div>
    </div>
);

const getDnsSettingsContent = (
    dnsAddresses: string[] | undefined,
    serverName?: string,
    portHttps?: number,
) => {
    const tlsAddress = dnsAddresses?.filter((addr: string) => addr.includes('tls://')) ?? [];
    const httpsAddress = dnsAddresses?.filter((addr: string) => addr.includes('https://')) ?? [];
    const quicAddress = dnsAddresses?.find((addr: string) => addr.includes('quic://'));

    const showDnsPrivacyNotice =
        !encryptionState.enabled && httpsAddress.length < 1 && tlsAddress.length < 1;

    return showDnsPrivacyNotice ? (
        <div class={s.guideParagraph}>
            {intl.getMessage('setup_dns_notice_new', {
                a: (text: string) => (
                    <A href={Paths.Encryption} class={s.dnsLink}>
                        {text}
                    </A>
                ),
            })}
        </div>
    ) : (
        <div class={s.dnsSettingsContent}>
            <div class={s.methodCard}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadge}>Enkripsi Aktif</span>
                    <h3 class={s.methodTitle}>Alamat Protokol DNS Terenkripsi</h3>
                </div>

                <Show when={tlsAddress.length > 0}>
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>DNS-over-TLS (DoT - Port 853):</span>
                        <div class={s.copyField}>
                            <CopiedText text={tlsAddress[0]} />
                        </div>
                    </div>
                </Show>

                <Show when={httpsAddress.length > 0}>
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>DNS-over-HTTPS (DoH - Port 443):</span>
                        <div class={s.copyField}>
                            <CopiedText text={httpsAddress[0]} />
                        </div>
                    </div>
                </Show>

                <Show when={quicAddress}>
                    <div class={s.codeHighlightBox}>
                        <span class={s.codeLabel}>DNS-over-QUIC (DoQ):</span>
                        <div class={s.copyField}>
                            <CopiedText text={quicAddress!} />
                        </div>
                    </div>
                </Show>
            </div>

            {renderDnsDevicesList()}

            <div class={s.methodCard} style={{ 'margin-top': '20px' }}>
                <div class={s.methodHeader}>
                    <span class={s.methodBadgeSecondary}>Apple</span>
                    <h3 class={s.methodTitle}>{intl.getMessage('setup_dns_privacy_ioc_mac')}</h3>
                </div>
                <p class={s.methodDesc}>{intl.getMessage('setup_devices_dns_macos_desc')}</p>
                <div class={s.mobileConfigContainer}>
                    <MobileConfigForm
                        initialValues={{
                            host: serverName || '',
                            clientId: '',
                            protocol: MOBILE_CONFIG_LINKS.DOH,
                            port: portHttps,
                        }}
                    />
                </div>
            </div>
        </div>
    );
};

const DnsPrivacyLayout = (props: PlatformLayoutProps) => (
    <div class={s.platformContainer}>
        <div class={s.title}>{intl.getMessage('dns_privacy')}</div>
        <div class={s.text}>
            {getDnsSettingsContent(props.dnsAddresses, props.serverName, props.portHttps)}
        </div>
    </div>
);

const getPlatformLayouts = (params: PlatformLayoutProps): PlatformLayouts => ({
    Android: {
        title: intl.getMessage('setup_devices_android_title'),
        icon: 'android',
        component: <AndroidLayout {...params} />,
    },
    iOS: {
        title: intl.getMessage('setup_devices_ios_title'),
        icon: 'ios',
        component: <IOSLayout {...params} />,
    },
    Windows: {
        title: intl.getMessage('setup_devices_windows_title'),
        icon: 'windows',
        component: <WindowsLayout {...params} />,
    },
    macOS: {
        title: intl.getMessage('setup_devices_macos_title'),
        icon: 'mac',
        component: <MacOSLayout {...params} />,
    },
    Router: {
        title: intl.getMessage('setup_devices_router_title'),
        icon: 'router',
        component: <RouterLayout {...params} />,
    },
    dns_privacy: {
        title: intl.getMessage('dns_privacy'),
        icon: 'dns_privacy',
        component: (
            <DnsPrivacyLayout
                serverName={params.serverName}
                portHttps={params.portHttps}
                dnsAddresses={params.dnsAddresses}
            />
        ),
    },
});

type Props = {
    dnsAddresses?: string[];
};

export const Guide = (props: Props) => {
    const serverName = () => encryptionState.server_name;
    const portHttps = () => Number(encryptionState.port_https) || 0;

    // Default to Android as it's the most queried mobile platform
    const [activeTabLabel, setActiveTabLabel] = createSignal('Android');

    const platformLayouts = () =>
        getPlatformLayouts({
            serverName: serverName(),
            portHttps: portHttps(),
            dnsAddresses: props.dnsAddresses,
        });

    const activeLayout = () => platformLayouts()[activeTabLabel() as keyof typeof platformLayouts];

    return (
        <div class={s.deviceSelectorContainer}>
            <p class={s.selectorDesc}>{intl.getMessage('device_type')}</p>

            <div class={s.tabBar}>
                <For each={Object.entries(platformLayouts())}>
                    {([key, value]) => {
                        const isActive = () => activeTabLabel() === key;
                        return (
                            <button
                                type="button"
                                class={cn(s.tabButton, isActive() && s.tabButtonActive)}
                                onClick={() => setActiveTabLabel(key)}
                            >
                                <Icon icon={value.icon} class={s.tabIcon} />
                                <span class={s.tabTitle}>{value.title}</span>
                            </button>
                        );
                    }}
                </For>
            </div>

            <Show when={activeLayout()}>
                <div class={s.deviceContent}>{activeLayout().component}</div>
            </Show>
        </div>
    );
};
