import { createSignal, onMount, Show } from 'solid-js';
import { customFetch } from 'panel/api/customFetch';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import s from './styles.module.pcss';

export interface TelegramStatusData {
    enabled: boolean;
    connected: boolean;
    bot_username: string;
    admin_chat_id: string;
    notify_threats: boolean;
    notify_ddos: boolean;
    total_alerts_sent: number;
    last_alert_time: string;
}

export const TelegramBotConfig = () => {
    const [enabled, setEnabled] = createSignal(false);
    const [botToken, setBotToken] = createSignal('');
    const [adminChatID, setAdminChatID] = createSignal('');
    const [notifyThreats, setNotifyThreats] = createSignal(true);
    const [notifyDDoS, setNotifyDDoS] = createSignal(true);
    const [notifyDailyReport, setNotifyDailyReport] = createSignal(true);

    const [connected, setConnected] = createSignal(false);
    const [botUsername, setBotUsername] = createSignal('');
    const [totalAlerts, setTotalAlerts] = createSignal(0);

    const [testing, setTesting] = createSignal(false);
    const [saving, setSaving] = createSignal(false);

    const loadStatus = async () => {
        try {
            const data = await customFetch<TelegramStatusData>('control/telegram/status');
            if (data) {
                setEnabled(data.enabled);
                setConnected(data.connected);
                setBotUsername(data.bot_username || '');
                setAdminChatID(data.admin_chat_id || '');
                setNotifyThreats(data.notify_threats ?? true);
                setNotifyDDoS(data.notify_ddos ?? true);
                setTotalAlerts(data.total_alerts_sent || 0);
            }
        } catch {
            // Keep initial fallback
        }
    };

    onMount(() => {
        loadStatus();
    });

    const handleSave = async () => {
        setSaving(true);
        try {
            await customFetch('control/telegram/config', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    enabled: enabled(),
                    bot_token: botToken(),
                    admin_chat_id: adminChatID(),
                    notify_threats: notifyThreats(),
                    notify_ddos: notifyDDoS(),
                    notify_daily_report: notifyDailyReport(),
                }),
            });
            addSuccessToast(
                enabled()
                    ? 'Bot Telegram aktif & terhubung!'
                    : 'Konfigurasi Bot Telegram berhasil disimpan.',
            );
            await loadStatus();
        } catch (e: any) {
            addErrorToast(e?.message || 'Gagal menyimpan konfigurasi bot.');
        } finally {
            setSaving(false);
        }
    };

    const handleTest = async () => {
        if (!adminChatID() && !botToken()) {
            addErrorToast('Masukkan Bot Token dan Chat ID terlebih dahulu.');
            return;
        }
        setTesting(true);
        try {
            const res = await customFetch<{ ok: boolean; message: string }>('control/telegram/test', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    bot_token: botToken(),
                    admin_chat_id: adminChatID(),
                }),
            });
            if (res && res.ok) {
                addSuccessToast('🚀 Pesan tes berhasil terkirim ke Telegram Anda!');
            }
        } catch (e: any) {
            addErrorToast(e?.message || 'Gagal mengirim pesan tes. Periksa Token & Chat ID.');
        } finally {
            setTesting(false);
        }
    };

    return (
        <div class={s.container}>
            {/* Header */}
            <div class={s.header}>
                <div class={s.titleGroup}>
                    <div class={s.iconBadge}>🤖</div>
                    <div>
                        <h3 class={s.title}>Telegram Security Bot & Remote Controller</h3>
                        <p class={s.subtitle}>
                            Dapatkan notifikasi instan saat ancaman malware/DDoS terdeteksi dan kendalikan server AdGuard Home
                            Anda secara remote lewat chat Telegram di HP.
                        </p>
                    </div>
                </div>
                <span class={`${s.badge} ${connected() ? s.badgeActive : s.badgeInactive}`}>
                    <span class={s.badgeDot} />
                    {connected() ? (botUsername() ? `@${botUsername()} ONLINE` : 'BOT ONLINE') : 'BOT OFFLINE'}
                </span>
            </div>

            {/* Master Switch */}
            <div class={s.switchGroup}>
                <div class={s.switchRow}>
                    <div class={s.switchText}>
                        <span class={s.switchLabel}>Aktifkan Telegram Bot Engine</span>
                        <span class={s.switchDesc}>
                            Jalankan bot long-polling untuk menerima perintah chat dan mengirim peringatan push secara realtime.
                        </span>
                    </div>
                    <input
                        type="checkbox"
                        checked={enabled()}
                        onChange={(e) => setEnabled(e.currentTarget.checked)}
                        style={{ width: '20px', height: '20px', cursor: 'pointer' }}
                    />
                </div>
            </div>

            {/* Credentials Inputs */}
            <div class={s.inputGroup}>
                <div class={s.inputRow}>
                    <label class={s.label}>
                        <span>Bot API Token</span>
                        <a
                            href="https://t.me/BotFather"
                            target="_blank"
                            rel="noopener noreferrer"
                            class={s.labelHelp}
                        >
                            Dapatkan Token dari @BotFather ↗
                        </a>
                    </label>
                    <input
                        type="password"
                        class={s.textInput}
                        placeholder="Contoh: 1234567890:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
                        value={botToken()}
                        onInput={(e) => setBotToken(e.currentTarget.value)}
                    />
                </div>

                <div class={s.inputRow}>
                    <label class={s.label}>
                        <span>Admin Telegram Chat ID</span>
                        <a
                            href="https://t.me/userinfobot"
                            target="_blank"
                            rel="noopener noreferrer"
                            class={s.labelHelp}
                        >
                            Cek ID Anda via @userinfobot ↗
                        </a>
                    </label>
                    <input
                        type="text"
                        class={s.textInput}
                        placeholder="Contoh: 123456789 (Hanya angka ID Anda)"
                        value={adminChatID()}
                        onInput={(e) => setAdminChatID(e.currentTarget.value)}
                    />
                </div>
            </div>

            {/* Notification Toggles */}
            <div class={s.switchGroup}>
                <div class={s.switchRow}>
                    <div class={s.switchText}>
                        <span class={s.switchLabel}>🚨 Peringatan Ancaman Malware & Phishing</span>
                        <span class={s.switchDesc}>
                            Kirim notifikasi seketika jika ada perangkat di jaringan yang mencoba membuka link berbahaya.
                        </span>
                    </div>
                    <input
                        type="checkbox"
                        checked={notifyThreats()}
                        onChange={(e) => setNotifyThreats(e.currentTarget.checked)}
                        style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                    />
                </div>

                <div class={s.switchRow}>
                    <div class={s.switchText}>
                        <span class={s.switchLabel}>⚡ Peringatan Serangan Rate Limit / DDoS</span>
                        <span class={s.switchDesc}>
                            Kirim notifikasi saat ada IP yang dibekukan karena lonjakan query mencurigakan.
                        </span>
                    </div>
                    <input
                        type="checkbox"
                        checked={notifyDDoS()}
                        onChange={(e) => setNotifyDDoS(e.currentTarget.checked)}
                        style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                    />
                </div>
            </div>

            {/* Commands Guide CheatSheet */}
            <div class={s.label} style={{ "margin-top": "20px", "margin-bottom": "8px" }}>
                <span>📖 Panduan Perintah Chat di Telegram</span>
            </div>
            <table class={s.commandsTable}>
                <thead>
                    <tr>
                        <th style={{ width: "200px" }}>Perintah</th>
                        <th>Fungsi</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class={s.commandCode}>/status</span></td>
                        <td>Melihat status server, Uptime, penggunaan RAM & CPU secara live.</td>
                    </tr>
                    <tr>
                        <td><span class={s.commandCode}>/unblock &lt;domain&gt;</span></td>
                        <td>Membuka blokir domain seketika (Contoh: <code>/unblock reddit.com</code>).</td>
                    </tr>
                    <tr>
                        <td><span class={s.commandCode}>/block &lt;domain&gt;</span></td>
                        <td>Memasukkan domain ke daftar blokir saat itu juga.</td>
                    </tr>
                    <tr>
                        <td><span class={s.commandCode}>/pause [menit]</span></td>
                        <td>Menjeda proteksi filter sementara (Contoh: <code>/pause 10</code>).</td>
                    </tr>
                    <tr>
                        <td><span class={s.commandCode}>/resume</span></td>
                        <td>Mengaktifkan kembali seluruh filter adblock.</td>
                    </tr>
                    <tr>
                        <td><span class={s.commandCode}>/ping</span></td>
                        <td>Menguji kecepatan respon bot Telegram.</td>
                    </tr>
                </tbody>
            </table>

            {/* Actions */}
            <div class={s.actions}>
                <button class={s.btnSecondary} onClick={handleTest} disabled={testing()}>
                    {testing() ? 'Mengirim...' : '🚀 Test Koneksi Bot'}
                </button>
                <button class={s.btnPrimary} onClick={handleSave} disabled={saving()}>
                    {saving() ? 'Menyimpan...' : '💾 Simpan Konfigurasi Bot'}
                </button>
            </div>
        </div>
    );
};
