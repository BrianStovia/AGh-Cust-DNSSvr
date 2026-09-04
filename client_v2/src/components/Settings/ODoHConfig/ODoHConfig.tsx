import { createSignal, onMount, For, Show } from 'solid-js';
import cn from 'clsx';
import { customFetch } from 'panel/api/customFetch';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import s from './styles.module.pcss';

export interface ODoHPreset {
    name: string;
    provider: string;
    relay_url: string;
    target_url: string;
    region: string;
    description: string;
}

export interface ODoHData {
    enabled: boolean;
    preset: string;
    relay_url: string;
    target_url: string;
    anonymity_score: string;
    active_relays: ODoHPreset[];
}

export const ODoHConfig = () => {
    const [enabled, setEnabled] = createSignal(false);
    const [preset, setPreset] = createSignal('cloudflare');
    const [relayUrl, setRelayUrl] = createSignal('https://odoh-relay.cloudflare.com/proxy');
    const [targetUrl, setTargetUrl] = createSignal('https://odoh.cloudflare-dns.com/dns-query');
    const [presets, setPresets] = createSignal<ODoHPreset[]>([]);
    const [saving, setSaving] = createSignal(false);

    onMount(async () => {
        try {
            const data = await customFetch<ODoHData>('control/odoh/status');
            if (data) {
                setEnabled(data.enabled);
                setPreset(data.preset || 'cloudflare');
                setRelayUrl(data.relay_url || 'https://odoh-relay.cloudflare.com/proxy');
                setTargetUrl(data.target_url || 'https://odoh.cloudflare-dns.com/dns-query');
                if (data.active_relays) {
                    setPresets(data.active_relays);
                }
            }
        } catch {
            // Use defaults if fetch falls back
        }
    });

    const handleSelectPreset = (p: ODoHPreset, key: string) => {
        setPreset(key);
        setRelayUrl(p.relay_url);
        setTargetUrl(p.target_url);
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await customFetch('control/odoh/configure', {
                method: 'POST',
                body: JSON.stringify({
                    enabled: enabled(),
                    preset: preset(),
                    relay_url: relayUrl(),
                    target_url: targetUrl(),
                }),
            });
            addSuccessToast(
                enabled()
                    ? 'Oblivious DoH (ODoH Relay) berhasil diaktifkan!'
                    : 'Konfigurasi ODoH berhasil disimpan.',
            );
        } catch {
            addErrorToast('Gagal menyimpan konfigurasi ODoH.');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div class={s.card}>
            <div class={s.header}>
                <div class={s.titleGroup}>
                    <span class={s.badge}>Zero-Knowledge Privacy</span>
                    <h2 class={s.title}>Oblivious DNS-over-HTTPS (ODoH Relay)</h2>
                    <p class={s.desc}>
                        Enkripsi query dua lapis dengan pemutusan identitas IP publik server melalui perantara Relay Proxy terverifikasi.
                    </p>
                </div>
                <div class={s.toggleArea}>
                    <label class={s.toggleSwitch}>
                        <input
                            type="checkbox"
                            checked={enabled()}
                            onChange={(e) => setEnabled(e.currentTarget.checked)}
                        />
                        <span class={s.slider} />
                    </label>
                </div>
            </div>

            {/* Visual Flow Topology */}
            <div class={s.flowContainer}>
                <div class={s.flowNode}>
                    <div class={s.nodeIcon}>💻</div>
                    <span class={s.nodeLabel}>Perangkat Klien</span>
                    <span class={s.nodeSub}>Query Terenkripsi</span>
                </div>
                <span class={s.flowArrow}>➔</span>
                <div class={s.flowNode}>
                    <div class={s.nodeIcon}>🛡️</div>
                    <span class={s.nodeLabel}>DNS SERVER BRST</span>
                    <span class={s.nodeSub}>HPKE Envelope</span>
                </div>
                <span class={s.flowArrow}>➔</span>
                <div class={s.flowNode}>
                    <div class={s.nodeIcon}>🌐</div>
                    <span class={s.nodeLabel}>ODoH Relay Proxy</span>
                    <span class={s.nodeSub}>IP Decoupled</span>
                </div>
                <span class={s.flowArrow}>➔</span>
                <div class={s.flowNode}>
                    <div class={s.nodeIcon}>⚡</div>
                    <span class={s.nodeLabel}>Target Resolver</span>
                    <span class={s.nodeSub}>Zero-Knowledge</span>
                </div>
            </div>

            <Show when={presets().length > 0}>
                <div class={s.presetsGrid}>
                    <For each={presets()}>
                        {(p, idx) => {
                            const key = idx() === 0 ? 'cloudflare' : idx() === 1 ? 'quad9' : 'apple';
                            return (
                                <div
                                    class={cn(s.presetCard, {
                                        [s.presetActive]: preset() === key,
                                    })}
                                    onClick={() => handleSelectPreset(p, key)}
                                >
                                    <span class={s.presetTitle}>{p.name}</span>
                                    <span class={s.presetRegion}>{p.region}</span>
                                    <p class={s.presetDesc}>{p.description}</p>
                                </div>
                            );
                        }}
                    </For>
                </div>
            </Show>

            {/* Custom URLs Config */}
            <div class={s.customInputs}>
                <div class={s.inputGroup}>
                    <label class={s.inputLabel}>ODoH Proxy Relay URL:</label>
                    <input
                        type="text"
                        class={s.textInput}
                        value={relayUrl()}
                        onInput={(e) => {
                            setRelayUrl(e.currentTarget.value);
                            setPreset('custom');
                        }}
                    />
                </div>
                <div class={s.inputGroup}>
                    <label class={s.inputLabel}>ODoH Target Resolver Endpoint:</label>
                    <input
                        type="text"
                        class={s.textInput}
                        value={targetUrl()}
                        onInput={(e) => {
                            setTargetUrl(e.currentTarget.value);
                            setPreset('custom');
                        }}
                    />
                </div>
            </div>

            <div class={s.actions}>
                <button
                    type="button"
                    class={s.saveBtn}
                    onClick={handleSave}
                    disabled={saving()}
                >
                    {saving() ? 'Menyimpan...' : 'Simpan Pengaturan ODoH'}
                </button>
            </div>
        </div>
    );
};
