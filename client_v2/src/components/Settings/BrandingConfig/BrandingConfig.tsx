import { createSignal, onMount, For } from 'solid-js';
import cn from 'clsx';
import {
    brandingState,
    COLOR_PRESETS,
    saveBranding,
    resetBranding,
    type ColorPreset,
} from 'panel/stores/branding';
import { addSuccessToast } from 'panel/stores/toasts';
import { Button } from 'panel/common/ui/Button';
import s from './styles.module.pcss';

export const BrandingConfig = () => {
    const [title, setTitle] = createSignal('');
    const [subtitle, setSubtitle] = createSignal('');
    const [presetId, setPresetId] = createSignal('electric');
    const [primaryColor, setPrimaryColor] = createSignal('#0d84f8');
    const [accentColor, setAccentColor] = createSignal('#00d4ef');
    const [glowColor, setGlowColor] = createSignal('0 0 16px rgba(13, 132, 248, 0.45)');

    onMount(() => {
        setTitle(brandingState.title);
        setSubtitle(brandingState.subtitle);
        setPresetId(brandingState.presetId);
        setPrimaryColor(brandingState.primaryColor);
        setAccentColor(brandingState.accentColor);
        setGlowColor(brandingState.glowColor);
    });

    const handleSelectPreset = (preset: ColorPreset) => {
        setPresetId(preset.id);
        setPrimaryColor(preset.primary);
        setAccentColor(preset.accent);
        setGlowColor(preset.glow);
    };

    const handleSave = () => {
        saveBranding({
            title: title().trim() || 'DNS SERVER',
            subtitle: subtitle().trim() || 'BRST',
            presetId: presetId(),
            primaryColor: primaryColor(),
            accentColor: accentColor(),
            glowColor: glowColor(),
        });

        addSuccessToast('Pengaturan tema & branding berhasil disimpan!');
    };

    const handleReset = () => {
        resetBranding();
        setTitle(brandingState.title);
        setSubtitle(brandingState.subtitle);
        setPresetId(brandingState.presetId);
        setPrimaryColor(brandingState.primaryColor);
        setAccentColor(brandingState.accentColor);
        setGlowColor(brandingState.glowColor);

        addSuccessToast('Branding berhasil dikembalikan ke default.');
    };

    return (
        <div class={s.brandingContainer}>
            <div class={s.brandingHeader}>
                <div class={s.titleArea}>
                    <span class={s.badge}>Kustomisasi</span>
                    <h2 class={s.title}>Tema &amp; Identitas Branding</h2>
                </div>
            </div>

            <p class={s.desc}>
                Sesuaikan nama brand server DNS Anda dan pilih skema palet warna aksen modern yang akan diterapkan di seluruh antarmuka secara instan.
            </p>

            {/* ── Live Preview Box ── */}
            <div class={s.previewSection}>
                <div class={s.previewLabel}>Pratinjau Logo Sidebar (Live Preview)</div>
                <div class={s.previewBox}>
                    <div class={s.previewLogoWrapper}>
                        <div
                            class={s.previewIconBox}
                            style={{
                                background: `linear-gradient(135deg, ${primaryColor()} 0%, ${accentColor()} 100%)`,
                                'box-shadow': glowColor(),
                            }}
                        >
                            <img
                                src="assets/logo.svg"
                                alt="Preview Logo"
                                class={s.previewImg}
                            />
                        </div>
                        <div class={s.previewTextCol}>
                            <span class={s.previewMainTitle}>
                                {title() || 'DNS SERVER'}
                            </span>
                            <span
                                class={s.previewSubTitle}
                                style={{
                                    background: `linear-gradient(90deg, ${primaryColor()}, ${accentColor()})`,
                                    '-webkit-background-clip': 'text',
                                    '-webkit-text-fill-color': 'transparent',
                                    'background-clip': 'text',
                                }}
                            >
                                {subtitle() || 'BRST'}
                            </span>
                        </div>
                    </div>
                    <span class={s.previewHint}>
                        ✨ Logo dan warna aksen di sidebar akan langsung menyesuaikan.
                    </span>
                </div>
            </div>

            {/* ── Form Inputs ── */}
            <div class={s.formGrid}>
                <div class={s.inputGroup}>
                    <label for="branding-title" class={s.inputLabel}>
                        Nama Utama Brand:
                    </label>
                    <input
                        id="branding-title"
                        type="text"
                        class={s.textInput}
                        value={title()}
                        onInput={(e) => setTitle(e.currentTarget.value)}
                        placeholder="Contoh: DNS SERVER, HOMELAB, MY DNS"
                        maxLength={30}
                    />
                </div>

                <div class={s.inputGroup}>
                    <label for="branding-subtitle" class={s.inputLabel}>
                        Tag / Subjudul Brand:
                    </label>
                    <input
                        id="branding-subtitle"
                        type="text"
                        class={s.textInput}
                        value={subtitle()}
                        onInput={(e) => setSubtitle(e.currentTarget.value)}
                        placeholder="Contoh: BRST, PRO, SECURE"
                        maxLength={20}
                    />
                </div>
            </div>

            {/* ── Color Theme Presets ── */}
            <div class={s.presetsSection}>
                <div class={s.inputLabel}>Pilih Skema Warna Aksen:</div>
                <div class={s.presetGrid}>
                    <For each={COLOR_PRESETS}>
                        {(preset) => {
                            const isSelected = () => presetId() === preset.id;

                            return (
                                <button
                                    type="button"
                                    class={cn(s.presetCard, isSelected() && s.presetCardActive)}
                                    onClick={() => handleSelectPreset(preset)}
                                >
                                    <div class={s.colorDots}>
                                        <div
                                            class={s.dotPrimary}
                                            style={{ background: preset.primary }}
                                        />
                                        <div
                                            class={s.dotAccent}
                                            style={{ background: preset.accent }}
                                        />
                                    </div>
                                    <span class={s.presetName}>{preset.name}</span>
                                </button>
                            );
                        }}
                    </For>
                </div>
            </div>

            {/* ── Action Buttons ── */}
            <div class={s.actionRow}>
                <Button variant="primary" size="medium" class={s.saveBtn} onClick={handleSave}>
                    Simpan Perubahan
                </Button>
                <Button variant="secondary" size="medium" class={s.resetBtn} onClick={handleReset}>
                    Kembalikan ke Default
                </Button>
            </div>
        </div>
    );
};
