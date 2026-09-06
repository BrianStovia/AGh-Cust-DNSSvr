import type { WhoisInfo } from './whoisInfo';
import type { DeviceInfo } from './deviceInfo';

/**
 * Auto-Client information
 */
export interface ClientAuto {
    /** IP address */
    ip?: string;
    /** Name */
    name?: string;
    /** The source of this information */
    source?: string;
    whois_info?: WhoisInfo;
    device?: DeviceInfo;
}
