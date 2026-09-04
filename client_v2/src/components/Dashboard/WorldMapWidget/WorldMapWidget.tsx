import { createSignal, onMount, For } from 'solid-js';
import cn from 'clsx';
import { customFetch } from 'panel/api/customFetch';
import s from './styles.module.pcss';

export interface GeoUpstream {
    address: string;
    name: string;
    country: string;
    country_code: string;
    lat: number;
    lng: number;
    latency_ms: number;
    status: string;
}

export const WorldMapWidget = () => {
    const [upstreams, setUpstreams] = createSignal<GeoUpstream[]>([
        {
            address: 'https://cloudflare-dns.com/dns-query',
            name: 'Cloudflare Anycast (Jakarta/Singapore)',
            country: 'Indonesia / Singapore',
            country_code: 'ID',
            lat: -6.2088,
            lng: 106.8456,
            latency_ms: 8,
            status: 'active',
        },
        {
            address: 'tls://dns.alidns.com',
            name: 'Alibaba Cloud DNS (Asia-Pacific)',
            country: 'Singapore / Hong Kong',
            country_code: 'SG',
            lat: 1.3521,
            lng: 103.8198,
            latency_ms: 14,
            status: 'active',
        },
        {
            address: 'tls://dot.pub',
            name: 'Tencent DNSPod Ultra',
            country: 'Shenzhen, China',
            country_code: 'CN',
            lat: 22.5431,
            lng: 114.0579,
            latency_ms: 38,
            status: 'active',
        },
        {
            address: 'tls://dns11.quad9.net',
            name: 'Quad9 Threat-Blocked DNS',
            country: 'Zurich, Switzerland',
            country_code: 'CH',
            lat: 47.3769,
            lng: 8.5417,
            latency_ms: 26,
            status: 'active',
        },
        {
            address: 'tls://adblock.dns.mullvad.net',
            name: 'Mullvad Privacy AdBlock',
            country: 'Gothenburg, Sweden',
            country_code: 'SE',
            lat: 57.7089,
            lng: 11.9746,
            latency_ms: 165,
            status: 'active',
        },
        {
            address: 'tls://ordns.he.net',
            name: 'Hurricane Electric Global Backbone',
            country: 'Fremont, California (US)',
            country_code: 'US',
            lat: 37.5485,
            lng: -121.9886,
            latency_ms: 178,
            status: 'active',
        },
    ]);

    onMount(async () => {
        try {
            const data = await customFetch<GeoUpstream[]>('control/stats/geo_upstream');
            if (Array.isArray(data) && data.length > 0) {
                setUpstreams(data);
            }
        } catch {
            // Keep default initial list if request falls back
        }
    });

    // Helper to calculate map position percentage from lat/lng
    const getMapPos = (lat: number, lng: number) => {
        const x = ((lng + 180) / 360) * 100;
        const y = ((90 - lat) / 180) * 100;
        return {
            left: `${Math.max(4, Math.min(96, x))}%`,
            top: `${Math.max(8, Math.min(92, y))}%`,
        };
    };

    return (
        <div class={s.mapWidgetContainer}>
            <div class={s.mapHeader}>
                <div class={s.titleArea}>
                    <span class={s.badge}>Jaringan Global</span>
                    <h2 class={s.title}>Peta Lalu Lintas &amp; Peering Upstream DNS</h2>
                </div>
            </div>

            {/* ── Interactive World Map Visualizer ── */}
            <div class={s.mapWrapper}>
                <svg class={s.worldSvg} viewBox="0 0 1000 500" preserveAspectRatio="none">
                    <defs>
                        <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
                            <path d="M 50 0 L 0 0 0 50" fill="none" stroke="rgba(13, 132, 248, 0.12)" stroke-width="1" />
                        </pattern>
                    </defs>
                    <rect width="1000" height="500" fill="url(#grid)" />
                    {/* World Continents Schematic Path */}
                    <path
                        d="M150,120 Q180,80 250,90 Q300,100 320,150 Q310,220 250,260 Q180,240 140,180 Z 
                           M240,280 Q290,290 320,350 Q300,420 260,460 Q220,400 230,320 Z 
                           M480,90 Q560,70 620,110 Q580,180 500,200 Q450,150 480,90 Z 
                           M480,210 Q560,220 580,310 Q540,420 480,440 Q440,340 450,250 Z 
                           M640,90 Q780,60 880,130 Q840,220 740,240 Q650,200 640,90 Z 
                           M720,290 Q820,280 860,340 Q830,420 760,420 Q700,380 720,290 Z"
                        fill="rgba(13, 132, 248, 0.08)"
                        stroke="rgba(0, 212, 239, 0.35)"
                        stroke-width="1.5"
                    />
                </svg>

                {/* Animated Pulsing Upstream Radar Nodes */}
                <For each={upstreams()}>
                    {(node) => {
                        const pos = getMapPos(node.lat, node.lng);
                        return (
                            <div class={s.node} style={pos} title={`${node.name} (${node.latency_ms} ms)`}>
                                <div class={s.pulseDot}>
                                    <div class={s.pulseRing} />
                                </div>
                                <div class={s.nodeLabel}>
                                    {node.country_code} · {node.latency_ms} ms
                                </div>
                            </div>
                        );
                    }}
                </For>
            </div>

            {/* ── Active Server Peering Grid ── */}
            <div class={s.upstreamGrid}>
                <For each={upstreams()}>
                    {(node) => (
                        <div class={s.upstreamCard}>
                            <div class={s.serverMeta}>
                                <span class={s.serverName}>{node.name}</span>
                                <span class={s.serverCountry}>{node.country}</span>
                            </div>
                            <span
                                class={cn(s.latencyBadge, {
                                    [s.fast]: node.latency_ms < 30,
                                    [s.normal]: node.latency_ms >= 30 && node.latency_ms < 100,
                                    [s.global]: node.latency_ms >= 100,
                                })}
                            >
                                {node.latency_ms} ms
                            </span>
                        </div>
                    )}
                </For>
            </div>
        </div>
    );
};
