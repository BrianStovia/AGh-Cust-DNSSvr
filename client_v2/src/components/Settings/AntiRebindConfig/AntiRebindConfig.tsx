import { createSignal, onMount, For, Show } from 'solid-js';
import cn from 'clsx';
import { customFetch } from 'panel/api/customFetch';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import s from './styles.module.pcss';

export interface RebindEvent {
    timestamp: string;
    domain: string;
    client_ip: string;
    target_ip: string;
    action: string;
}

export interface RebindConfigData {
    enabled: boolean;
    strict_mode: boolean;
    whitelisted_domains: string[];
    blocked_count: number;
    recent_attacks: RebindEvent[];
}

export const AntiRebindConfig = () => {
    const [enabled, setEnabled] = createSignal(true);
    const [strictMode, setStrictMode] = createSignal(true);
    const [whitelist, setWhitelist] = createSignal<string[]>([
        'local',
        'lan',
        'home',
        'home.arpa',
        'internal',
        'intranet',
        'corp',
        'lab',
        'plex.direct',
        'myfritz.net',
        'ts.net',
    ]);
    const [blockedCount, setBlockedCount] = createSignal(0);
    const [recentAttacks, setRecentAttacks] = createSignal<RebindEvent[]>([]);
    const [newDomain, setNewDomain] = createSignal('');
    const [saving, setSaving] = createSignal(false);

    const loadStatus = async () => {
        try {
            const data = await customFetch<RebindConfigData>('control/rebind/status');
            if (data) {
                setEnabled(data.enabled);
                setStrictMode(data.strict_mode);
                if (data.whitelisted_domains) {
                    setWhitelist(data.whitelisted_domains);
                }
                setBlockedCount(data.blocked_count || 0);
                if (data.recent_attacks) {
                    setRecentAttacks(data.recent_attacks);
                }
            }
        } catch {
            // Keep initial fallback
        }
    };

    onMount(() => {
        loadStatus();
    });

    const handleAddDomain = (domainToAdd?: string) => {
        const val = (domainToAdd || newDomain()).trim().toLowerCase().replace(/^\*\./, '').replace(/^\./, '');
        if (!val) return;
        if (!whitelist().includes(val)) {
            setWhitelist([...whitelist(), val]);
            if (!domainToAdd) {
                setNewDomain('');
            }
            addSuccessToast(`Domain "${val}" ditambahkan ke whitelist pengecualian.`);
        }
    };

    const handleRemoveDomain = (domainToRemove: string) => {
        setWhitelist(whitelist().filter((d) => d !== domainToRemove));
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await customFetch('control/rebind/config', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    enabled: enabled(),
                    strict_mode: strictMode(),
                    whitelisted_domains: whitelist(),
                }),
            });
            addSuccessToast(
                enabled()
                    ? 'Proteksi Anti-DNS Rebinding & Local Shield aktif!'
                    : 'Konfigurasi Anti-DNS Rebinding berhasil disimpan.',
            );
            await loadStatus();
        } catch (err: unknown) {
            const errorMsg = err instanceof Error ? err.message : String(err);
            addErrorToast({ error: errorMsg });
        } finally {
            setSaving(false);
        }
    };

    const handleClearStats = async () => {
        try {
            await customFetch('control/rebind/clear_stats', {
                method: 'POST',
            });
            setBlockedCount(0);
            setRecentAttacks([]);
            addSuccessToast('Statistik & riwayat log serangan berhasil dibersihkan.');
        } catch (err: unknown) {
            const errorMsg = err instanceof Error ? err.message : String(err);
            addErrorToast({ error: errorMsg });
        }
    };

    return (
        <div class={s.container}>
            {/* Header & Status */}
            <div class={s.header}>
                <div class={s.titleGroup}>
                    <div class={s.iconBadge}>🛡️</div>
                    <div>
                        <h3 class={s.title}>Anti-DNS Rebinding & Local Network Shield</h3>
                        <p class={s.subtitle}>
                            Mencegah serangan siber di mana situs publik dari internet mencoba meresolusi ke alamat IP privat lokal
                            (127.0.0.1, 192.168.x.x, 10.x.x.x, fe80::) untuk mengeksploitasi Router, NAS, CCTV, atau perangkat IoT Anda.
                        </p>
                    </div>
                </div>
                <div class={cn(s.badge, enabled() ? s.badgeActive : s.badgeInactive)}>
                    <span class={s.badgeDot} />
                    {enabled() ? 'SHIELD AKTIF' : 'NONAKTIF'}
                </div>
            </div>

            {/* Metrics Row */}
            <div class={s.statsGrid}>
                <div class={s.statCard}>
                    <div class={s.statLabel}>Total Serangan Dicegah</div>
                    <div class={cn(s.statValue, blockedCount() > 0 && s.statDanger)}>{blockedCount()}</div>
                    <div class={s.statDesc}>Percobaan Rebinding yang diintersepsi</div>
                </div>
                <div class={s.statCard}>
                    <div class={s.statLabel}>Subnet Terproteksi</div>
                    <div class={s.statValueHighlight}>RFC 1918 & ULA</div>
                    <div class={s.statDesc}>127.0.0.0/8, 10/8, 172.16/12, 192.168/16, fc00::/7</div>
                </div>
                <div class={s.statCard}>
                    <div class={s.statLabel}>Pengecualian Whitelist</div>
                    <div class={s.statValue}>{whitelist().length}</div>
                    <div class={s.statDesc}>Domain lokal resmi yang diizinkan</div>
                </div>
            </div>

            {/* Controls Section */}
            <div class={s.controlSection}>
                <label class={s.toggleRow}>
                    <input
                        type="checkbox"
                        checked={enabled()}
                        onChange={(e) => setEnabled(e.currentTarget.checked)}
                        class={s.checkbox}
                    />
                    <div class={s.toggleContent}>
                        <span class={s.toggleTitle}>Aktifkan Proteksi Anti-DNS Rebinding</span>
                        <span class={s.toggleDesc}>
                            Blokir otomatis seluruh respon DNS dari upstream publik yang meresolusi ke alamat IP lokal / privat.
                        </span>
                    </div>
                </label>

                <Show when={enabled()}>
                    <label class={s.toggleRow}>
                        <input
                            type="checkbox"
                            checked={strictMode()}
                            onChange={(e) => setStrictMode(e.currentTarget.checked)}
                            class={s.checkbox}
                        />
                        <div class={s.toggleContent}>
                            <span class={s.toggleTitle}>Mode Ketat (Strict Zero-Trust Drop)</span>
                            <span class={s.toggleDesc}>
                                Langsung jatuhkan (Drop/NXDOMAIN) respon IP privat yang tidak terdaftar dalam daftar whitelist domain.
                            </span>
                        </div>
                    </label>
                </Show>
            </div>

            {/* Whitelist Management */}
            <div class={s.whitelistSection}>
                <div class={s.sectionHeader}>
                    <h4 class={s.sectionTitle}>Whitelist Pengecualian Domain (Legitimate Local Domains)</h4>
                    <span class={s.sectionSubtitle}>
                        Domain-domain berikut diizinkan meresolusi IP privat (misal layanan homelab atau Plex lokal).
                    </span>
                </div>

                <div class={s.chipsContainer}>
                    <For each={whitelist()}>
                        {(domain) => (
                            <span class={s.chip}>
                                <span class={s.chipText}>*.{domain}</span>
                                <button
                                    type="button"
                                    class={s.chipRemove}
                                    onClick={() => handleRemoveDomain(domain)}
                                    title="Hapus domain dari whitelist"
                                >
                                    &times;
                                </button>
                            </span>
                        )}
                    </For>
                </div>

                <div class={s.addDomainForm}>
                    <input
                        type="text"
                        placeholder="Tambahkan domain (misal: myhomelab.local, plex.direct)..."
                        value={newDomain()}
                        onInput={(e) => setNewDomain(e.currentTarget.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                                e.preventDefault();
                                handleAddDomain();
                            }
                        }}
                        class={s.input}
                    />
                    <button
                        type="button"
                        class={s.btnAdd}
                        onClick={() => handleAddDomain()}
                        disabled={!newDomain().trim()}
                    >
                        + Tambah Domain
                    </button>
                </div>

                {/* Quick Presets */}
                <div class={s.quickPresets}>
                    <span class={s.presetLabel}>Preset Cepat:</span>
                    <button
                        type="button"
                        class={s.btnPreset}
                        onClick={() => handleAddDomain('plex.direct')}
                    >
                        + plex.direct
                    </button>
                    <button
                        type="button"
                        class={s.btnPreset}
                        onClick={() => handleAddDomain('myfritz.net')}
                    >
                        + myfritz.net
                    </button>
                    <button
                        type="button"
                        class={s.btnPreset}
                        onClick={() => handleAddDomain('ts.net')}
                    >
                        + ts.net (Tailscale)
                    </button>
                    <button
                        type="button"
                        class={s.btnPreset}
                        onClick={() => handleAddDomain('synology.me')}
                    >
                        + synology.me (NAS)
                    </button>
                </div>
            </div>

            {/* Recent Attacks Log */}
            <Show when={recentAttacks().length > 0}>
                <div class={s.attacksSection}>
                    <div class={s.attacksHeader}>
                        <h4 class={s.sectionTitle}>Log Percobaan Serangan Rebinding Terbaru</h4>
                        <button
                            type="button"
                            class={s.btnClear}
                            onClick={handleClearStats}
                        >
                            Bersihkan Log
                        </button>
                    </div>
                    <div class={s.tableWrapper}>
                        <table class={s.attackTable}>
                            <thead>
                                <tr>
                                    <th>Waktu</th>
                                    <th>Domain Berbahaya</th>
                                    <th>IP Klien</th>
                                    <th>Target IP Privat</th>
                                    <th>Aksi</th>
                                </tr>
                            </thead>
                            <tbody>
                                <For each={recentAttacks()}>
                                    {(evt) => (
                                        <tr>
                                            <td class={s.tdTime}>{evt.timestamp}</td>
                                            <td class={s.tdDomain}>{evt.domain}</td>
                                            <td class={s.tdClient}>{evt.client_ip}</td>
                                            <td class={s.tdTargetIP}>{evt.target_ip}</td>
                                            <td>
                                                <span class={s.actionBadge}>{evt.action}</span>
                                            </td>
                                        </tr>
                                    )}
                                </For>
                            </tbody>
                        </table>
                    </div>
                </div>
            </Show>

            {/* Footer Action */}
            <div class={s.footer}>
                <button
                    type="button"
                    class={s.btnSave}
                    onClick={handleSave}
                    disabled={saving()}
                >
                    {saving() ? 'Menyimpan Konfigurasi...' : 'Simpan Konfigurasi Shield'}
                </button>
            </div>
        </div>
    );
};
