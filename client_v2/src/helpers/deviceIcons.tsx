import { Show } from 'solid-js';
import type { DeviceInfo } from 'panel/initialState';
import { Tooltip } from 'panel/common/ui/Tooltip';

export const getDeviceEmoji = (icon?: string, deviceType?: string): string => {
    switch (icon) {
        case 'apple':
            return '🍎';
        case 'windows':
            return '🪟';
        case 'android':
            return '🤖';
        case 'tv':
            return '📺';
        case 'game':
            return '🎮';
        case 'linux':
            return '🐧';
        case 'iot':
            return '💡';
        case 'speaker':
            return '🔊';
        case 'camera':
            return '📹';
        case 'printer':
            return '🖨️';
        case 'nas':
            return '💾';
        default:
            break;
    }

    switch (deviceType) {
        case 'phone':
            return '📱';
        case 'pc':
            return '💻';
        case 'tv':
            return '📺';
        case 'gameconsole':
            return '🎮';
        case 'iot':
            return '💡';
        case 'nas':
            return '💾';
        case 'camera':
            return '📹';
        case 'printer':
            return '🖨️';
        case 'audio':
            return '🔊';
        default:
            return '🌐';
    }
};

export const getDeviceTheme = (icon?: string): { bg: string; text: string; border: string } => {
    switch (icon) {
        case 'apple':
            return { bg: 'rgba(160, 160, 170, 0.15)', text: '#e4e4e7', border: 'rgba(160, 160, 170, 0.3)' };
        case 'windows':
            return { bg: 'rgba(56, 189, 248, 0.15)', text: '#38bdf8', border: 'rgba(56, 189, 248, 0.3)' };
        case 'android':
            return { bg: 'rgba(34, 197, 94, 0.15)', text: '#4ade80', border: 'rgba(34, 197, 94, 0.3)' };
        case 'tv':
            return { bg: 'rgba(234, 179, 8, 0.15)', text: '#facc15', border: 'rgba(234, 179, 8, 0.3)' };
        case 'game':
            return { bg: 'rgba(168, 85, 247, 0.15)', text: '#c084fc', border: 'rgba(168, 85, 247, 0.3)' };
        case 'linux':
            return { bg: 'rgba(249, 115, 22, 0.15)', text: '#fb923c', border: 'rgba(249, 115, 22, 0.3)' };
        case 'iot':
            return { bg: 'rgba(236, 72, 153, 0.15)', text: '#f472b6', border: 'rgba(236, 72, 153, 0.3)' };
        case 'speaker':
            return { bg: 'rgba(20, 184, 166, 0.15)', text: '#2dd4bf', border: 'rgba(20, 184, 166, 0.3)' };
        case 'camera':
            return { bg: 'rgba(239, 68, 68, 0.15)', text: '#f87171', border: 'rgba(239, 68, 68, 0.3)' };
        case 'printer':
            return { bg: 'rgba(99, 102, 241, 0.15)', text: '#818cf8', border: 'rgba(99, 102, 241, 0.3)' };
        case 'nas':
            return { bg: 'rgba(6, 182, 212, 0.15)', text: '#22d3ee', border: 'rgba(6, 182, 212, 0.3)' };
        default:
            return { bg: 'rgba(113, 113, 122, 0.15)', text: '#a1a1aa', border: 'rgba(113, 113, 122, 0.3)' };
    }
};

type BadgeProps = {
    device?: DeviceInfo;
    compact?: boolean;
};

export const DeviceBadge = (props: BadgeProps) => {
    if (!props.device) return null;

    const emoji = () => getDeviceEmoji(props.device?.icon, props.device?.device_type);
    const theme = () => getDeviceTheme(props.device?.icon);

    const tooltipContent = () => (
        <div style={{ "min-width": "180px", "font-size": "12px", "line-height": "1.5" }}>
            <div style={{ "font-weight": "600", "margin-bottom": "4px", "display": "flex", "align-items": "center", "gap": "6px" }}>
                <span>{emoji()}</span>
                <span>{props.device?.model || props.device?.name}</span>
            </div>
            <div style={{ "color": "var(--text-secondary, #94a3b8)", "font-size": "11px" }}>
                <div><strong>Vendor:</strong> {props.device?.vendor || 'Unknown'}</div>
                <div><strong>OS / Platform:</strong> {props.device?.os || 'Unknown'}</div>
                <div><strong>Confidence:</strong> {props.device?.confidence || 0}%</div>
                <Show when={props.device?.matched_rule}>
                    <div><strong>Signature:</strong> {props.device?.matched_rule}</div>
                </Show>
                <Show when={props.device?.query_count}>
                    <div><strong>Queries Analyzed:</strong> {props.device?.query_count}</div>
                </Show>
            </div>
        </div>
    );

    if (props.compact) {
        return (
            <Tooltip content={tooltipContent()}>
                <span
                    style={{
                        "display": "inline-flex",
                        "align-items": "center",
                        "justify-content": "center",
                        "background": theme().bg,
                        "border": `1px solid ${theme().border}`,
                        "border-radius": "6px",
                        "padding": "2px 6px",
                        "font-size": "13px",
                        "cursor": "help",
                        "line-height": "1",
                        "margin-right": "6px",
                    }}
                >
                    {emoji()}
                </span>
            </Tooltip>
        );
    }

    return (
        <Tooltip content={tooltipContent()}>
            <span
                style={{
                    "display": "inline-flex",
                    "align-items": "center",
                    "gap": "5px",
                    "background": theme().bg,
                    "color": theme().text,
                    "border": `1px solid ${theme().border}`,
                    "border-radius": "6px",
                    "padding": "3px 8px",
                    "font-size": "12px",
                    "font-weight": "500",
                    "cursor": "help",
                    "line-height": "1.3",
                    "white-space": "nowrap",
                }}
            >
                <span>{emoji()}</span>
                <span>{props.device.model || props.device.os}</span>
                <span style={{ "font-size": "10px", "opacity": "0.75" }}>({props.device.confidence}%)</span>
            </span>
        </Tooltip>
    );
};
