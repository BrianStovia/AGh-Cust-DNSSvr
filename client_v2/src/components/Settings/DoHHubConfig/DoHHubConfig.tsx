import { createSignal, onMount, Show } from 'solid-js';
import { customFetch } from 'panel/api/customFetch';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import s from './styles.module.pcss';

export interface DoHInfo {
    host: string;
    doh_url: string;
    doh_client_template: string;
    dot_url: string;
    doq_url: string;
    routes: string[];
    tls_active: boolean;
    status: string;
}

export const DoHHubConfig = () => {
    const [info, setInfo] = createSignal<DoHInfo>({
        host: window.location.hostname,
        doh_url: `${window.location.protocol}//${window.location.host}/dns-query`,
        doh_client_template: `${window.location.protocol}//${window.location.host}/dns-query/{nama_perangkat}`,
        dot_url: `tls://${window.location.hostname}:853`,
        doq_url: `quic://${window.location.hostname}:853`,
        routes: ['/dns-query', '/dns-query/{ClientID}'],
        tls_active: false,
        status: 'active',
    });
    const [testing, setTesting] = createSignal(false);
    const [testResult, setTestResult] = createSignal<string | null>(null);

    onMount(async () => {
        try {
            const data = await customFetch<DoHInfo>('control/doh/info');
            if (data && data.doh_url) {
                setInfo(data);
            }
        } catch {
            // Keep initial fallback
        }
    });

    const copyText = (text: string, label: string) => {
        navigator.clipboard.writeText(text);
        addSuccessToast(`${label} berhasil disalin ke clipboard!`);
    };

    const handleTestDoH = async () => {
        setTesting(true);
        setTestResult(null);
        try {
            const res = await customFetch<{ status: string; message: string; latency_ms: number }>(
                'control/doh/test',
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ domain: 'google.com' }),
                },
            );
            if (res && res.status === 'ok') {
                setTestResult(`✓ ${res.message} (${res.latency_ms} ms)`);
                addSuccessToast('DoH Endpoint aktif dan merespon dengan cepat!');
            }
        } catch (err) {
            addErrorToast({
                error: err instanceof Error ? err.message : 'Gagal menguji DoH endpoint.',
            });
        } finally {
            setTesting(false);
        }
    };

    return (
        <div class={s.card}>
            <div class={s.header}>
                <div class={s.titleGroup}>
                    <span class={s.badge}>Encrypted DNS Hub</span>
                    <h2 class={s.title}>DNS-over-HTTPS (DoH) &amp; Endpoint Kustom</h2>
                    <p class={s.desc}>
                        Gunakan endpoint DoH terenkripsi ini untuk menghubungkan browser (Chrome, Firefox), Android Private DNS, dan Windows 11 tanpa melewati Port 53 plaintext.
                    </p>
                </div>
            </div>

            <div class={s.grid}>
                {/* Standard DoH */}
                <div class={s.urlBox}>
                    <span class={s.urlTitle}>URL DoH Standar (Browser &amp; OS)</span>
                    <div class={s.codeRow}>
                        <span class={s.codeText}>{info().doh_url}</span>
                        <button
                            type="button"
                            class={s.copyBtn}
                            onClick={() => copyText(info().doh_url, 'URL DoH Standar')}
                        >
                            Salin
                        </button>
                    </div>
                </div>

                {/* DoH with Client Identification */}
                <div class={s.urlBox}>
                    <span class={s.urlTitle}>URL DoH dengan Nama Perangkat (Client ID)</span>
                    <div class={s.codeRow}>
                        <span class={s.codeText}>{info().doh_client_template}</span>
                        <button
                            type="button"
                            class={s.copyBtn}
                            onClick={() =>
                                copyText(info().doh_client_template, 'Template DoH Client ID')
                            }
                        >
                            Salin
                        </button>
                    </div>
                </div>

                {/* DoT */}
                <div class={s.urlBox}>
                    <span class={s.urlTitle}>DNS-over-TLS (DoT Endpoint)</span>
                    <div class={s.codeRow}>
                        <span class={s.codeText}>{info().dot_url}</span>
                        <button
                            type="button"
                            class={s.copyBtn}
                            onClick={() => copyText(info().dot_url, 'DoT Endpoint')}
                        >
                            Salin
                        </button>
                    </div>
                </div>

                {/* DoQ */}
                <div class={s.urlBox}>
                    <span class={s.urlTitle}>DNS-over-QUIC (DoQ Endpoint)</span>
                    <div class={s.codeRow}>
                        <span class={s.codeText}>{info().doq_url}</span>
                        <button
                            type="button"
                            class={s.copyBtn}
                            onClick={() => copyText(info().doq_url, 'DoQ Endpoint')}
                        >
                            Salin
                        </button>
                    </div>
                </div>
            </div>

            <div class={s.guideSection}>
                <h4 class={s.guideTitle}>Cara Pemasangan di Browser &amp; Smartphone:</h4>
                <ul class={s.guideList}>
                    <li>
                        <strong>Google Chrome &amp; Edge:</strong> Pengaturan ➔ Privasi &amp; Keamanan ➔ Gunakan DNS Aman ➔ Pilih <em>Kustom</em> ➔ Tempelkan <code>{info().doh_url}</code>
                    </li>
                    <li>
                        <strong>Mozilla Firefox:</strong> Pengaturan ➔ Privasi &amp; Keamanan ➔ DNS over HTTPS ➔ Perlindungan Maksimal ➔ Tempelkan <code>{info().doh_url}</code>
                    </li>
                    <li>
                        <strong>Android (Private DNS):</strong> Pengaturan ➔ Jaringan &amp; Internet ➔ Private DNS ➔ Masukkan hostname <code>{info().host}</code>
                    </li>
                </ul>
            </div>

            <div class={s.actions}>
                <button
                    type="button"
                    class={s.testBtn}
                    onClick={handleTestDoH}
                    disabled={testing()}
                >
                    {testing() ? 'Menguji Endpoint...' : 'Uji DoH Endpoint Sekarang ➔'}
                </button>
                <Show when={testResult()}>
                    <div class={s.testResult}>{testResult()}</div>
                </Show>
            </div>
        </div>
    );
};
