import { createMemo, Show, For, type JSX } from 'solid-js';

import { dashboardState } from 'panel/stores/dashboard';
import { Guide } from 'panel/common/ui/Guide/Guide';

import theme from 'panel/lib/theme';

import intl from 'panel/common/intl';
import { CopiedText } from 'panel/common/ui/CopiedText/CopiedText';
import s from './SetupGuide.module.pcss';

type Props = {
    dnsAddresses?: string[];
    isStep?: boolean;
    footer?: JSX.Element;
};

export const SetupGuide = (props: Props) => {
    const dnsAddresses = createMemo(() => props.dnsAddresses ?? dashboardState.dnsAddresses ?? []);

    const encryptedAddresses = createMemo(() =>
        dnsAddresses().filter(
            (address: string) =>
                address.includes('https://') ||
                address.includes('tls://') ||
                address.includes('quic://'),
        ),
    );

    const plainAddresses = createMemo(() =>
        dnsAddresses().filter(
            (address: string) =>
                !address.includes('https://') &&
                !address.includes('tls://') &&
                !address.includes('quic://'),
        ),
    );

    return (
        <div class={props.isStep ? s.stepRoot : theme.layout.container}>
            <div class={s.header}>
                <h1 class={s.pageTitle}>
                    {props.isStep
                        ? intl.getMessage('setup_guide_title')
                        : intl.getMessage('setup_guide')}
                </h1>
                <Show when={!props.isStep}>
                    <div class={s.pageDesc}>{intl.getMessage('setup_guide_desc')}</div>
                </Show>
            </div>

            <div class={s.guidePage}>
                <Show when={!props.isStep}>
                    <h1 class={s.guideTitle}>{intl.getMessage('setup_guide_device_type')}</h1>
                </Show>
                <Guide dnsAddresses={dnsAddresses()} />

                <div class={s.guideDesc}>
                    <h1 class={s.dnsTitle}>{intl.getMessage('home_dns_addresses')}</h1>

                    <p>{intl.getMessage('home_dns_addresses_desc')}</p>

                    <Show when={encryptedAddresses().length > 0}>
                        <div class={s.dnsSubtitle}>
                            {intl.getMessage('encrypted_dns_addresses')}
                        </div>

                        <div class={s.protocolGrid}>
                            <For each={encryptedAddresses()}>
                                {(addr) => {
                                    const isDot = addr.startsWith('tls://');
                                    const isDoh = addr.startsWith('https://');
                                    const isDoq = addr.startsWith('quic://');

                                    return (
                                        <div class={s.protocolCard}>
                                            <div class={s.protocolHeader}>
                                                <span class={s.protocolBadge}>
                                                    {isDot && 'DoT (Port 853)'}
                                                    {isDoh && 'DoH (Port 443)'}
                                                    {isDoq && 'DoQ (Port 853)'}
                                                    {!isDot && !isDoh && !isDoq && 'TLS'}
                                                </span>
                                                <h4 class={s.protocolTitle}>
                                                    {isDot && 'DNS-over-TLS'}
                                                    {isDoh && 'DNS-over-HTTPS'}
                                                    {isDoq && 'DNS-over-QUIC'}
                                                    {!isDot && !isDoh && !isDoq && 'Encrypted DNS'}
                                                </h4>
                                            </div>
                                            <p class={s.protocolDesc}>
                                                {isDot && 'Digunakan oleh router, Stubby, atau upstream DNS. Khusus Private DNS Android, gunakan hanya nama domain (tanpa tls:// atau port).'}
                                                {isDoh && 'Format universal terenkripsi untuk peramban (Chrome, Firefox), iOS, dan aplikasi DNS mobile.'}
                                                {isDoq && 'Protokol kueri modern berbasis UDP QUIC dengan koneksi instan dan latensi minimal.'}
                                            </p>
                                            <div class={s.codeBox}>
                                                <CopiedText text={addr} />
                                            </div>
                                        </div>
                                    );
                                }}
                            </For>
                        </div>
                    </Show>

                    <Show when={plainAddresses().length > 0}>
                        <div class={s.dnsSubtitle}>{intl.getMessage('plain_dns_addresses')}</div>

                        <div class={s.protocolCard}>
                            <div class={s.protocolHeader}>
                                <span class={s.protocolBadgeSecondary}>Port 53 (UDP/TCP)</span>
                                <h4 class={s.protocolTitle}>DNS Standar Lokal</h4>
                            </div>
                            <p class={s.protocolDesc}>
                                Digunakan untuk konfigurasi DNS statis pada router Wi-Fi lokal atau perangkat tanpa dukungan enkripsi.
                            </p>
                            <div class={s.ipGrid}>
                                <For each={plainAddresses()}>
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
            </div>

            <div class={s.footer}>{props.footer}</div>
        </div>
    );
};
