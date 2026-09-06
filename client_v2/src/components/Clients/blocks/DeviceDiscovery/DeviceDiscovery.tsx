import { createSignal, createMemo, onMount, For, Show } from 'solid-js';
import cn from 'clsx';

import type { DeviceInfo } from 'panel/initialState';
import { DeviceBadge, getDeviceEmoji } from 'panel/helpers/deviceIcons';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import { getClients } from 'panel/stores/dashboard';
import { Loader } from 'panel/common/ui/Loader';
import { Table, type TableColumn } from 'panel/common/ui/Table';
import theme from 'panel/lib/theme';
import s from './DeviceDiscovery.module.pcss';

type DetectedResponse = {
    total_devices: number;
    summary: Record<string, number>;
    devices: DeviceInfo[];
};

export const DeviceDiscovery = () => {
    const [loading, setLoading] = createSignal(false);
    const [devices, setDevices] = createSignal<DeviceInfo[]>([]);
    const [summary, setSummary] = createSignal<Record<string, number>>({});
    const [totalDevices, setTotalDevices] = createSignal(0);
    const [selectedCategory, setSelectedCategory] = createSignal<string>('all');
    const [searchQuery, setSearchQuery] = createSignal('');
    const [convertingIp, setConvertingIp] = createSignal<string | null>(null);

    const loadDevices = async () => {
        setLoading(true);
        try {
            const res = await fetch('/control/devices/detected');
            if (res.ok) {
                const data: DetectedResponse = await res.json();
                setDevices(data.devices || []);
                setSummary(data.summary || {});
                setTotalDevices(data.total_devices || 0);
            }
        } catch (e) {
            console.error('Failed to load detected devices:', e);
        } finally {
            setLoading(false);
        }
    };

    const handleClear = async () => {
        if (!confirm('Are you sure you want to clear the device discovery cache?')) return;
        try {
            const res = await fetch('/control/devices/clear', { method: 'POST' });
            if (res.ok) {
                addSuccessToast('Device discovery cache cleared');
                loadDevices();
            }
        } catch (e) {
            addErrorToast('Failed to clear device discovery cache');
        }
    };

    const handleConvert = async (dev: DeviceInfo) => {
        setConvertingIp(dev.ip);
        try {
            const tags = [];
            if (dev.device_type) tags.push(`device_${dev.device_type}`);
            if (dev.icon === 'apple') tags.push('os_ios');
            else if (dev.icon === 'windows') tags.push('os_windows');
            else if (dev.icon === 'android') tags.push('os_android');
            else if (dev.icon === 'linux') tags.push('os_linux');

            const res = await fetch('/control/devices/convert', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ip: dev.ip,
                    name: dev.model || dev.name || `Device ${dev.ip}`,
                    tags: tags,
                }),
            });

            if (res.ok) {
                addSuccessToast(`Saved "${dev.model || dev.ip}" as Persistent Client!`);
                getClients();
            } else {
                const err = await res.json();
                addErrorToast(err.message || 'Failed to save persistent client');
            }
        } catch (e) {
            addErrorToast('Error saving persistent client');
        } finally {
            setConvertingIp(null);
        }
    };

    onMount(() => {
        loadDevices();
    });

    const filteredDevices = createMemo(() => {
        const cat = selectedCategory();
        const query = searchQuery().toLowerCase().trim();

        return devices().filter((d) => {
            const matchCat =
                cat === 'all' ||
                d.device_type === cat ||
                d.icon === cat;

            const matchQuery =
                !query ||
                d.ip.toLowerCase().includes(query) ||
                d.name.toLowerCase().includes(query) ||
                d.model.toLowerCase().includes(query) ||
                d.vendor.toLowerCase().includes(query) ||
                d.os.toLowerCase().includes(query) ||
                (d.client_id && d.client_id.toLowerCase().includes(query));

            return matchCat && matchQuery;
        });
    });

    const categories = createMemo(() => [
        { id: 'all', label: 'All Devices', count: totalDevices(), emoji: '🔍' },
        { id: 'phone', label: 'Smartphones & Tablets', count: (summary()['phone'] || 0), emoji: '📱' },
        { id: 'pc', label: 'Computers & PCs', count: (summary()['pc'] || 0), emoji: '💻' },
        { id: 'tv', label: 'Smart TVs & Media', count: (summary()['tv'] || 0), emoji: '📺' },
        { id: 'gameconsole', label: 'Gaming Consoles', count: (summary()['gameconsole'] || 0), emoji: '🎮' },
        { id: 'iot', label: 'Smart Home & IoT', count: (summary()['iot'] || 0), emoji: '💡' },
        { id: 'nas', label: 'NAS Storage', count: (summary()['nas'] || 0), emoji: '💾' },
        { id: 'camera', label: 'IP Cameras', count: (summary()['camera'] || 0), emoji: '📹' },
        { id: 'audio', label: 'Smart Speakers', count: (summary()['audio'] || 0), emoji: '🔊' },
    ]);

    const columns = createMemo<TableColumn<DeviceInfo>[]>(() => [
        {
            key: 'device',
            header: { text: 'Device & Hardware' },
            accessor: 'model',
            sortable: true,
            render: (_val: unknown, row: DeviceInfo) => (
                <div style={{ display: 'flex', 'flex-direction': 'column', gap: '4px' }}>
                    <div style={{ display: 'flex', 'align-items': 'center', gap: '8px' }}>
                        <DeviceBadge device={row} />
                    </div>
                    <span style={{ 'font-size': '11px', color: 'var(--text-secondary, #94a3b8)' }}>
                        Vendor: <strong>{row.vendor || 'Unknown'}</strong>
                    </span>
                </div>
            ),
        },
        {
            key: 'ip',
            header: { text: 'Network Identifier' },
            accessor: 'ip',
            sortable: true,
            render: (value: string, row: DeviceInfo) => (
                <div style={{ display: 'flex', 'flex-direction': 'column', gap: '2px' }}>
                    <span style={{ 'font-weight': '600', color: '#38bdf8' }}>{value}</span>
                    <Show when={row.client_id}>
                        <span style={{ 'font-size': '11px', color: 'var(--text-secondary, #94a3b8)' }}>
                            CID: {row.client_id}
                        </span>
                    </Show>
                </div>
            ),
        },
        {
            key: 'os',
            header: { text: 'OS / Platform' },
            accessor: 'os',
            sortable: true,
            render: (value: string) => <span>{value || 'Embedded'}</span>,
        },
        {
            key: 'confidence',
            header: { text: 'Confidence' },
            accessor: 'confidence',
            sortable: true,
            render: (value: number, row: DeviceInfo) => (
                <div style={{ width: '110px' }}>
                    <div style={{ display: 'flex', 'justify-content': 'space-between', 'font-size': '11px' }}>
                        <span style={{ color: value >= 90 ? '#4ade80' : '#facc15', 'font-weight': '600' }}>
                            {value}%
                        </span>
                        <span style={{ color: 'var(--text-secondary, #94a3b8)' }}>
                            {row.query_count || 1} queries
                        </span>
                    </div>
                    <div class={s.confidenceBar}>
                        <div class={s.confidenceFill} style={{ width: `${value}%` }} />
                    </div>
                </div>
            ),
        },
        {
            key: 'signature',
            header: { text: 'Trigger Signature' },
            accessor: 'matched_rule',
            sortable: true,
            render: (value: string, row: DeviceInfo) => (
                <div style={{ display: 'flex', 'flex-direction': 'column', gap: '2px', 'max-width': '220px' }}>
                    <span style={{ 'font-size': '12px', 'font-weight': '500' }}>{value}</span>
                    <Show when={row.matched_domain}>
                        <span
                            class={theme.common.textOverflow}
                            style={{ 'font-size': '10px', color: '#94a3b8' }}
                            title={row.matched_domain}
                        >
                            {row.matched_domain}
                        </span>
                    </Show>
                </div>
            ),
        },
        {
            key: 'actions',
            header: { text: 'Action' },
            sortable: false,
            render: (_val: unknown, row: DeviceInfo) => (
                <button
                    type="button"
                    class={cn(s.actionBtn, s.actionBtnPrimary)}
                    style={{ padding: '6px 12px', 'font-size': '12px' }}
                    onClick={() => handleConvert(row)}
                    disabled={convertingIp() === row.ip}
                >
                    <Show when={convertingIp() === row.ip} fallback="💾 Save Client">
                        Saving...
                    </Show>
                </button>
            ),
        },
    ]);

    return (
        <div class={s.container}>
            <div class={s.headerRow}>
                <div class={s.desc}>
                    AdGuard Home passively monitors DNS telemetry, captive portal probes (Apple, Microsoft, Android), and vendor endpoints to automatically discover and profile all connected smartphones, PCs, smart TVs, consoles, and IoT devices on your network.
                </div>
                <div class={s.btnGroup}>
                    <button type="button" class={s.actionBtn} onClick={loadDevices}>
                        🔄 Refresh
                    </button>
                    <button type="button" class={cn(s.actionBtn, s.actionBtnDanger)} onClick={handleClear}>
                        🗑️ Clear Cache
                    </button>
                </div>
            </div>

            {/* Quick Category Filter Cards */}
            <div class={s.statsGrid}>
                <For each={categories()}>
                    {(cat) => (
                        <div
                            class={cn(s.statCard, selectedCategory() === cat.id && s.statCardActive)}
                            onClick={() => setSelectedCategory(cat.id)}
                        >
                            <div class={s.statHeader}>
                                <span class={s.statEmoji}>{cat.emoji}</span>
                                <span class={s.statCount}>{cat.count}</span>
                            </div>
                            <span class={s.statLabel}>{cat.label}</span>
                        </div>
                    )}
                </For>
            </div>

            {/* Filter Search Bar */}
            <div class={s.actionsBar}>
                <div class={s.searchBox}>
                    <input
                        type="text"
                        class={s.searchInput}
                        value={searchQuery()}
                        onInput={(e) => setSearchQuery(e.currentTarget.value)}
                        placeholder="Filter by IP, device name, model, vendor, or OS..."
                    />
                </div>
                <span style={{ 'font-size': '13px', color: 'var(--text-secondary, #94a3b8)' }}>
                    Showing <strong>{filteredDevices().length}</strong> of <strong>{totalDevices()}</strong> discovered devices
                </span>
            </div>

            {/* Discovered Devices Table */}
            <div class={s.tableSection}>
                <Show
                    when={!loading()}
                    fallback={
                        <div style={{ padding: '48px', display: 'flex', 'justify-content': 'center' }}>
                            <Loader color="green" />
                        </div>
                    }
                >
                    <Show
                        when={filteredDevices().length > 0}
                        fallback={
                            <div class={s.emptyState}>
                                <span style={{ 'font-size': '36px', 'margin-bottom': '12px', display: 'block' }}>📡</span>
                                <h3 style={{ 'font-size': '16px', 'font-weight': '600', color: 'var(--text-primary, #f8fafc)', 'margin-bottom': '4px' }}>
                                    No Devices Discovered Yet
                                </h3>
                                <p style={{ 'font-size': '13px' }}>
                                    Devices will appear here automatically as they send DNS queries through AdGuard Home.
                                </p>
                            </div>
                        }
                    >
                        <Table<DeviceInfo>
                            data={filteredDevices()}
                            columns={columns()}
                            pageSize={25}
                            getRowId={(row) => row.ip}
                        />
                    </Show>
                </Show>
            </div>
        </div>
    );
};
