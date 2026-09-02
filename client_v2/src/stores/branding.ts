import { createStore } from 'solid-js/store';

export type ColorPreset = {
    id: string;
    name: string;
    primary: string;
    accent: string;
    glow: string;
};

export const COLOR_PRESETS: ColorPreset[] = [
    {
        id: 'electric',
        name: 'Electric Blue (Default BRST)',
        primary: '#0d84f8',
        accent: '#00d4ef',
        glow: '0 0 16px rgba(13, 132, 248, 0.45)',
    },
    {
        id: 'violet',
        name: 'Neon Violet & Fuchsia',
        primary: '#8b5cf6',
        accent: '#d946ef',
        glow: '0 0 16px rgba(139, 92, 246, 0.45)',
    },
    {
        id: 'emerald',
        name: 'Emerald Matrix & Teal',
        primary: '#10b981',
        accent: '#06b6d4',
        glow: '0 0 16px rgba(16, 185, 129, 0.45)',
    },
    {
        id: 'amber',
        name: 'Cyber Amber & Orange',
        primary: '#f59e0b',
        accent: '#ea580c',
        glow: '0 0 16px rgba(245, 158, 11, 0.45)',
    },
    {
        id: 'crimson',
        name: 'Crimson Flare & Rose',
        primary: '#ef4444',
        accent: '#f43f5e',
        glow: '0 0 16px rgba(239, 68, 68, 0.45)',
    },
];

export type BrandingConfig = {
    title: string;
    subtitle: string;
    presetId: string;
    primaryColor: string;
    accentColor: string;
    glowColor: string;
};

const DEFAULT_BRANDING: BrandingConfig = {
    title: 'DNS SERVER',
    subtitle: 'BRST',
    presetId: 'electric',
    primaryColor: '#0d84f8',
    accentColor: '#00d4ef',
    glowColor: '0 0 16px rgba(13, 132, 248, 0.45)',
};

const STORAGE_KEY = 'agh_branding_config';

const [state, setState] = createStore<BrandingConfig>({ ...DEFAULT_BRANDING });

export const brandingState = state;

export const applyBrandingTheme = (config: BrandingConfig) => {
    if (typeof document === 'undefined') return;

    const root = document.documentElement;
    root.style.setProperty('--brst-primary-50', config.primaryColor);
    root.style.setProperty('--product-primary-50', config.primaryColor);
    root.style.setProperty('--brst-cyan-50', config.accentColor);
    root.style.setProperty('--product-secondary-50', config.accentColor);
    root.style.setProperty('--brst-glow-primary', config.glowColor);
};

export const initBranding = () => {
    if (typeof window === 'undefined') return;

    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            const parsed = JSON.parse(saved) as Partial<BrandingConfig>;
            const merged: BrandingConfig = {
                title: parsed.title || DEFAULT_BRANDING.title,
                subtitle: parsed.subtitle || DEFAULT_BRANDING.subtitle,
                presetId: parsed.presetId || DEFAULT_BRANDING.presetId,
                primaryColor: parsed.primaryColor || DEFAULT_BRANDING.primaryColor,
                accentColor: parsed.accentColor || DEFAULT_BRANDING.accentColor,
                glowColor: parsed.glowColor || DEFAULT_BRANDING.glowColor,
            };
            setState(merged);
            applyBrandingTheme(merged);
            return;
        }
    } catch (e) {
        console.warn('Failed to load branding from localStorage:', e);
    }

    applyBrandingTheme(DEFAULT_BRANDING);
};

export const saveBranding = (config: Partial<BrandingConfig>) => {
    const updated: BrandingConfig = {
        title: config.title ?? state.title,
        subtitle: config.subtitle ?? state.subtitle,
        presetId: config.presetId ?? state.presetId,
        primaryColor: config.primaryColor ?? state.primaryColor,
        accentColor: config.accentColor ?? state.accentColor,
        glowColor: config.glowColor ?? state.glowColor,
    };

    setState(updated);
    applyBrandingTheme(updated);

    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    } catch (e) {
        console.error('Failed to save branding to localStorage:', e);
    }
};

export const resetBranding = () => {
    setState({ ...DEFAULT_BRANDING });
    applyBrandingTheme(DEFAULT_BRANDING);

    try {
        localStorage.removeItem(STORAGE_KEY);
    } catch (e) {
        console.error('Failed to clear branding in localStorage:', e);
    }
};

// Automatically initialize branding on module import
initBranding();

