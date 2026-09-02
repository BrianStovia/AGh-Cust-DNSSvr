import { createSignal, createMemo, For, Show } from 'solid-js';
import cn from 'clsx';
import { dnsConfigState, setDnsConfig } from 'panel/stores/dnsConfig';
import { splitByNewLine } from 'panel/helpers/helpers';
import { Button } from 'panel/common/ui/Button';
import { Switch } from 'panel/common/controls/Switch';
import {
    ROUTING_PRESETS,
    parseUpstreamList,
    isPresetActive,
    togglePresetInUpstreams,
    addCustomRouteRule,
    removeRouteRule,
    type RoutingPreset,
} from 'panel/helpers/smartRouting';
import s from './styles.module.pcss';

export const SmartRoutingCard = () => {
    const [customDomain, setCustomDomain] = createSignal('');
    const [customTarget, setCustomTarget] = createSignal('');
    const [isSubmitting, setIsSubmitting] = createSignal(false);

    const currentLines = () => splitByNewLine(dnsConfigState.upstream_dns || '');

    const parsedData = createMemo(() => parseUpstreamList(currentLines()));
    const routingRules = createMemo(() => parsedData().routingRules);

    const handleTogglePreset = async (preset: RoutingPreset, checked: boolean) => {
        setIsSubmitting(true);
        try {
            const updated = togglePresetInUpstreams(currentLines(), preset.id, checked);
            await setDnsConfig(
                { upstream_dns: updated.join('\n') },
                {
                    toastMessage: checked
                        ? `${preset.name} berhasil diaktifkan!`
                        : `${preset.name} telah dinonaktifkan.`,
                },
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleAddCustomRule = async (e: Event) => {
        e.preventDefault();
        const domain = customDomain().trim();
        const target = customTarget().trim();
        if (!domain || !target) return;

        setIsSubmitting(true);
        try {
            const updated = addCustomRouteRule(currentLines(), domain, target);
            await setDnsConfig(
                { upstream_dns: updated.join('\n') },
                { toastMessage: `Rute khusus untuk ${domain} berhasil ditambahkan!` },
            );
            setCustomDomain('');
            setCustomTarget('');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleRemoveRule = async (rawLine: string) => {
        setIsSubmitting(true);
        try {
            const updated = removeRouteRule(currentLines(), rawLine);
            await setDnsConfig(
                { upstream_dns: updated.join('\n') },
                { toastMessage: 'Rute khusus berhasil dihapus.' },
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div class={s.smartCard}>
            <div class={s.cardHeader}>
                <div class={s.titleArea}>
                    <span class={s.badge}>Perutean Pintar</span>
                    <h3 class={s.title}>Smart Upstream Routing &amp; Game Accelerator</h3>
                </div>
            </div>

            <p class={s.desc}>
                Pisahkan jalur DNS secara otomatis berdasarkan kebutuhan trafik: minimalkan *ping latency* untuk game online, percepat *buffering* video streaming ke CDN lokal, atau arahkan domain tertentu ke upstream pilihan.
            </p>

            {/* ── Preset Cards ── */}
            <div class={s.presetsGrid}>
                <For each={ROUTING_PRESETS}>
                    {(preset) => {
                        const active = () => isPresetActive(currentLines(), preset.id);

                        return (
                            <div class={cn(s.presetCard, active() && s.presetCardActive)}>
                                <div>
                                    <div class={s.presetTop}>
                                        <span class={s.presetIcon}>{preset.icon}</span>
                                        <Switch
                                            id={`preset-switch-${preset.id}`}
                                            checked={active()}
                                            disabled={isSubmitting()}
                                            onChange={(e) => {
                                                const target = e.target as HTMLInputElement;
                                                handleTogglePreset(preset, target.checked);
                                            }}
                                        />
                                    </div>
                                    <div class={s.presetName}>{preset.name}</div>
                                    <div class={s.presetDesc}>{preset.description}</div>
                                </div>
                                <div class={s.presetFooter}>
                                    <span style={{ 'font-size': '11px', color: 'var(--default-description-text)' }}>
                                        Target Upstream:
                                    </span>
                                    <span class={s.presetTarget}>{preset.defaultUpstream}</span>
                                </div>
                            </div>
                        );
                    }}
                </For>
            </div>

            {/* ── Custom Domain Route Form ── */}
            <div class={s.customSection}>
                <div class={s.customTitle}>
                    <span>➕ Tambah Perutean Domain Kustom</span>
                </div>
                <form onSubmit={handleAddCustomRule} class={s.formRow}>
                    <input
                        type="text"
                        class={s.inputField}
                        placeholder="Nama Domain (misal: *.kantor.lan atau riot.com)"
                        value={customDomain()}
                        onInput={(e) => setCustomDomain(e.currentTarget.value)}
                        required
                    />
                    <input
                        type="text"
                        class={s.inputField}
                        placeholder="Target DNS Server (misal: 192.168.1.1 atau 1.1.1.1)"
                        value={customTarget()}
                        onInput={(e) => setCustomTarget(e.currentTarget.value)}
                        required
                    />
                    <Button
                        type="submit"
                        variant="primary"
                        size="medium"
                        disabled={isSubmitting() || !customDomain().trim() || !customTarget().trim()}
                    >
                        Tambah Rute
                    </Button>
                </form>
            </div>

            {/* ── Active Routing Rules ── */}
            <div>
                <div class={s.customTitle}>
                    <span>📋 Daftar Rute Aktif ({routingRules().length})</span>
                </div>

                <Show
                    when={routingRules().length > 0}
                    fallback={
                        <div class={s.emptyNotice}>
                            Belum ada perutean domain khusus yang aktif. Aktifkan salah satu preset di atas atau tambahkan rute kustom.
                        </div>
                    }
                >
                    <div class={s.rulesList}>
                        <For each={routingRules()}>
                            {(rule) => (
                                <div class={s.ruleItem}>
                                    <div class={s.ruleLeft}>
                                        <Show when={rule.isPreset}>
                                            <span class={s.rulePresetBadge}>
                                                {rule.presetId === 'gaming' ? '🎮 Game' : rule.presetId === 'streaming' ? '🎬 Stream' : '🇮🇩 Domestik'}
                                            </span>
                                        </Show>
                                        <div class={s.ruleDomains}>
                                            <For each={rule.domains.slice(0, 4)}>
                                                {(d) => <span class={s.domainTag}>{d}</span>}
                                            </For>
                                            <Show when={rule.domains.length > 4}>
                                                <span class={s.domainTag}>+{rule.domains.length - 4} lainnya</span>
                                            </Show>
                                        </div>
                                        <span class={s.arrowIcon}>➔</span>
                                        <span class={s.targetTag}>{rule.upstream}</span>
                                    </div>
                                    <button
                                        type="button"
                                        class={s.deleteBtn}
                                        onClick={() => handleRemoveRule(rule.rawLine)}
                                        disabled={isSubmitting()}
                                        title="Hapus perutean ini"
                                    >
                                        Hapus
                                    </button>
                                </div>
                            )}
                        </For>
                    </div>
                </Show>
            </div>
        </div>
    );
};
