// Country code and flag utility for GeoIP & DNS Query Logs

const COUNTRY_NAME_TO_CODE: Record<string, string> = {
    indonesia: 'ID',
    'united states': 'US',
    usa: 'US',
    singapore: 'SG',
    malaysia: 'MY',
    japan: 'JP',
    germany: 'DE',
    'united kingdom': 'GB',
    uk: 'GB',
    netherlands: 'NL',
    australia: 'AU',
    canada: 'CA',
    france: 'FR',
    russia: 'RU',
    china: 'CN',
    india: 'IN',
    'south korea': 'KR',
    korea: 'KR',
    brazil: 'BR',
    vietnam: 'VN',
    thailand: 'TH',
    philippines: 'PH',
    taiwan: 'TW',
    'hong kong': 'HK',
    switzerland: 'CH',
    sweden: 'SE',
    italy: 'IT',
    spain: 'ES',
    poland: 'PL',
    ukraine: 'UA',
    finland: 'FI',
    norway: 'NO',
    denmark: 'DK',
    ireland: 'IE',
    austria: 'AT',
    belgium: 'BE',
    turkey: 'TR',
    'saudi arabia': 'SA',
    'united arab emirates': 'AE',
    uae: 'AE',
    egypt: 'EG',
    'south africa': 'ZA',
    argentina: 'AR',
    mexico: 'MX',
    chile: 'CL',
    colombia: 'CO',
    'new zealand': 'NZ',
};

const CCTLD_TO_COUNTRY: Record<string, { code: string; name: string }> = {
    id: { code: 'ID', name: 'Indonesia' },
    sg: { code: 'SG', name: 'Singapore' },
    my: { code: 'MY', name: 'Malaysia' },
    jp: { code: 'JP', name: 'Japan' },
    kr: { code: 'KR', name: 'South Korea' },
    cn: { code: 'CN', name: 'China' },
    tw: { code: 'TW', name: 'Taiwan' },
    hk: { code: 'HK', name: 'Hong Kong' },
    th: { code: 'TH', name: 'Thailand' },
    vn: { code: 'VN', name: 'Vietnam' },
    ph: { code: 'PH', name: 'Philippines' },
    in: { code: 'IN', name: 'India' },
    au: { code: 'AU', name: 'Australia' },
    nz: { code: 'NZ', name: 'New Zealand' },
    us: { code: 'US', name: 'United States' },
    uk: { code: 'GB', name: 'United Kingdom' },
    de: { code: 'DE', name: 'Germany' },
    fr: { code: 'FR', name: 'France' },
    nl: { code: 'NL', name: 'Netherlands' },
    ru: { code: 'RU', name: 'Russia' },
    ca: { code: 'CA', name: 'Canada' },
    br: { code: 'BR', name: 'Brazil' },
    it: { code: 'IT', name: 'Italy' },
    es: { code: 'ES', name: 'Spain' },
    ch: { code: 'CH', name: 'Switzerland' },
    se: { code: 'SE', name: 'Sweden' },
    no: { code: 'NO', name: 'Norway' },
    fi: { code: 'FI', name: 'Finland' },
    dk: { code: 'DK', name: 'Denmark' },
    pl: { code: 'PL', name: 'Poland' },
    ua: { code: 'UA', name: 'Ukraine' },
    ie: { code: 'IE', name: 'Ireland' },
    at: { code: 'AT', name: 'Austria' },
    be: { code: 'BE', name: 'Belgium' },
    tr: { code: 'TR', name: 'Turkey' },
    sa: { code: 'SA', name: 'Saudi Arabia' },
    ae: { code: 'AE', name: 'UAE' },
    za: { code: 'ZA', name: 'South Africa' },
    mx: { code: 'MX', name: 'Mexico' },
    ar: { code: 'AR', name: 'Argentina' },
};

/**
 * Converts a 2-letter ISO country code into an emoji flag
 */
export const isoCodeToFlag = (isoCode: string): string => {
    if (!isoCode || isoCode.length !== 2) return '';
    const upper = isoCode.toUpperCase();
    const firstChar = upper.charCodeAt(0);
    const secondChar = upper.charCodeAt(1);

    if (firstChar < 65 || firstChar > 90 || secondChar < 65 || secondChar > 90) {
        return '';
    }

    return String.fromCodePoint(0x1f1e6 + firstChar - 65, 0x1f1e6 + secondChar - 65);
};

/**
 * Converts a country name or code to its Unicode emoji flag
 */
export const getCountryFlag = (countryOrCode?: string | null): string => {
    if (!countryOrCode) return '';
    const trimmed = countryOrCode.trim();

    // If already 2 letters, treat as ISO code
    if (trimmed.length === 2) {
        return isoCodeToFlag(trimmed);
    }

    // Lookup by country name
    const lower = trimmed.toLowerCase();
    const mapped = COUNTRY_NAME_TO_CODE[lower];
    if (mapped) {
        return isoCodeToFlag(mapped);
    }

    return '';
};

/**
 * Inspects a domain to detect if it has a ccTLD and returns the corresponding flag
 */
export const getDomainCountryFlag = (
    domain?: string | null,
): { flag: string; code: string; name: string } | null => {
    if (!domain) return null;

    const parts = domain.toLowerCase().split('.');
    if (parts.length < 2) return null;

    const lastPart = parts[parts.length - 1];
    const country = CCTLD_TO_COUNTRY[lastPart];

    if (country) {
        const flag = isoCodeToFlag(country.code);
        if (flag) {
            return {
                flag,
                code: country.code,
                name: country.name,
            };
        }
    }

    return null;
};
