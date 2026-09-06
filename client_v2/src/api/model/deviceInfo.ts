export interface DeviceInfo {
    ip: string;
    client_id?: string;
    name: string;
    device_type: string;
    os: string;
    vendor: string;
    model: string;
    icon: string;
    confidence: number;
    matched_rule: string;
    matched_domain: string;
    first_seen: number;
    last_seen: number;
    query_count: number;
}
