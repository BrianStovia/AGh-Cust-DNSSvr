// Helper for Smart Upstream Routing & Game Accelerator

export type RoutingPreset = {
    id: string;
    name: string;
    icon: string;
    description: string;
    domains: string[];
    defaultUpstream: string;
};

export const ROUTING_PRESETS: RoutingPreset[] = [
    {
        id: 'gaming',
        name: 'Game Accelerator (Ultra-Low Latency)',
        icon: '🎮',
        description: 'Rute tercepat tanpa filter berat untuk Steam, Valorant, Mobile Legends, Epic, Roblox, PSN, Xbox',
        domains: [
            'steampowered.com',
            'steamcommunity.com',
            'valve.net',
            'riotgames.com',
            'leagueoflegends.com',
            'pvp.net',
            'epicgames.com',
            'mobilelegends.com',
            'mihoyo.com',
            'hoyoverse.com',
            'roblox.com',
            'playstation.net',
            'xboxlive.com',
        ],
        defaultUpstream: '1.1.1.1',
    },
    {
        id: 'streaming',
        name: 'Streaming CDN Optimizer (4K Buffering)',
        icon: '🎬',
        description: 'Optimasi CDN lokal Indonesia untuk Netflix, YouTube, Disney+, Spotify',
        domains: [
            'netflix.com',
            'nflxvideo.net',
            'googlevideo.com',
            'ytimg.com',
            'disneyplus.com',
            'spotify.com',
            'fast.com',
        ],
        defaultUpstream: 'https://1.1.1.1/dns-query',
    },
    {
        id: 'domestic',
        name: 'Rute Domestik Indonesia (.id)',
        icon: '🌐',
        description: 'Rute langsung untuk seluruh domain Indonesia (.id, co.id, go.id, perbankan)',
        domains: ['id', 'co.id', 'go.id', 'ac.id', 'web.id'],
        defaultUpstream: '1.1.1.1',
    },
];

export type ParsedRouteRule = {
    id: string;
    domains: string[];
    upstream: string;
    rawLine: string;
    isPreset?: boolean;
    presetId?: string;
};

/**
 * Builds the AdGuard Home upstream syntax for a list of domains and upstream:
 * [/domain1.com/domain2.com/]1.1.1.1
 */
export const buildUpstreamRule = (domains: string[], upstream: string): string => {
    const cleanDomains = domains.map((d) => d.trim().replace(/^\*\./, '')).filter(Boolean);
    const cleanUpstream = upstream.trim();
    if (!cleanDomains.length || !cleanUpstream) return '';

    return `[/${cleanDomains.join('/')}/]${cleanUpstream}`;
};

/**
 * Parses upstream_dns lines into standard upstreams and domain-specific routing rules
 */
export const parseUpstreamList = (upstreamDns: string[] = []): {
    standardUpstreams: string[];
    routingRules: ParsedRouteRule[];
} => {
    const standardUpstreams: string[] = [];
    const routingRules: ParsedRouteRule[] = [];

    for (const raw of upstreamDns) {
        const line = raw.trim();
        if (!line) continue;

        if (line.startsWith('[/')) {
            const closingSlashIdx = line.indexOf('/]', 2);
            if (closingSlashIdx !== -1) {
                const domainStr = line.substring(2, closingSlashIdx);
                const upstream = line.substring(closingSlashIdx + 2).trim();
                const domains = domainStr.split('/').filter(Boolean);

                // Identify if it matches any known preset
                let presetId: string | undefined;
                for (const p of ROUTING_PRESETS) {
                    if (domains.length === p.domains.length && p.domains.every((d) => domains.includes(d))) {
                        presetId = p.id;
                        break;
                    }
                }

                routingRules.push({
                    id: line,
                    domains,
                    upstream,
                    rawLine: line,
                    isPreset: !!presetId,
                    presetId,
                });
                continue;
            }
        }

        standardUpstreams.push(line);
    }

    return { standardUpstreams, routingRules };
};

/**
 * Checks if a specific preset is currently active
 */
export const isPresetActive = (upstreamDns: string[], presetId: string): boolean => {
    const { routingRules } = parseUpstreamList(upstreamDns);
    return routingRules.some((r) => r.presetId === presetId);
};

/**
 * Toggles a preset on or off in the upstream list
 */
export const togglePresetInUpstreams = (
    currentUpstreams: string[],
    presetId: string,
    enable: boolean,
    customUpstream?: string,
): string[] => {
    const preset = ROUTING_PRESETS.find((p) => p.id === presetId);
    if (!preset) return currentUpstreams;

    const { standardUpstreams, routingRules } = parseUpstreamList(currentUpstreams);

    // Filter out this preset if already present
    const remainingRules = routingRules.filter((r) => r.presetId !== presetId);

    if (enable) {
        const upstreamToUse = customUpstream?.trim() || preset.defaultUpstream;
        const newRule = buildUpstreamRule(preset.domains, upstreamToUse);
        return [...standardUpstreams, ...remainingRules.map((r) => r.rawLine), newRule];
    }

    return [...standardUpstreams, ...remainingRules.map((r) => r.rawLine)];
};

/**
 * Adds a custom domain routing rule
 */
export const addCustomRouteRule = (
    currentUpstreams: string[],
    domain: string,
    targetUpstream: string,
): string[] => {
    const cleanDomain = domain.trim().toLowerCase().replace(/^\*\./, '');
    const cleanUpstream = targetUpstream.trim();
    if (!cleanDomain || !cleanUpstream) return currentUpstreams;

    const newRule = buildUpstreamRule([cleanDomain], cleanUpstream);
    const exists = currentUpstreams.some((u) => u.trim() === newRule);
    if (exists) return currentUpstreams;

    return [...currentUpstreams, newRule];
};

/**
 * Removes a routing rule by its raw line
 */
export const removeRouteRule = (currentUpstreams: string[], rawLineToRemove: string): string[] => {
    return currentUpstreams.filter((u) => u.trim() !== rawLineToRemove.trim());
};
