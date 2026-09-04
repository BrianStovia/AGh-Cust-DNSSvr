import { createSignal, onMount, For, Show } from 'solid-js';
import { customFetch } from 'panel/api/customFetch';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import s from './styles.module.pcss';

export interface UnblockItem {
    id: string;
    domain: string;
    client_ip: string;
    name: string;
    reason: string;
    status: string;
    created_at: string;
}

export const UnblockRequests = () => {
    const [requests, setRequests] = createSignal<UnblockItem[]>([]);
    const [loading, setLoading] = createSignal(false);

    const loadRequests = async () => {
        setLoading(true);
        try {
            const data = await customFetch<UnblockItem[]>('control/unblock_requests');
            if (Array.isArray(data)) {
                setRequests(data);
            }
        } catch {
            // Ignore error if request fails
        } finally {
            setLoading(false);
        }
    };

    onMount(() => {
        loadRequests();
    });

    const handleApprove = async (item: UnblockItem) => {
        try {
            await customFetch('control/unblock_requests/approve', {
                method: 'POST',
                body: JSON.stringify({ id: item.id, domain: item.domain }),
            });
            addSuccessToast(`Domain ${item.domain} berhasil dibuka blokirnya!`);
            loadRequests();
        } catch {
            addErrorToast(`Gagal membuka blokir untuk ${item.domain}`);
        }
    };

    const handleDelete = async (id: string) => {
        try {
            await customFetch('control/unblock_requests/delete', {
                method: 'POST',
                body: JSON.stringify({ id }),
            });
            addSuccessToast('Permintaan dihapus.');
            loadRequests();
        } catch {
            addErrorToast('Gagal menghapus permintaan.');
        }
    };

    return (
        <div class={s.card}>
            <div class={s.header}>
                <div class={s.titleGroup}>
                    <span class={s.badge}>Interactive Defense</span>
                    <h2 class={s.title}>Permohonan Buka Blokir (Request Unblock)</h2>
                    <p class={s.desc}>
                        Kelola permintaan pembukaan blokir domain yang diajukan oleh pengguna jaringan melalui Custom Block Page.
                    </p>
                </div>
                <a
                    href="/blocked.html?domain=contoh-situs-terblokir.com"
                    target="_blank"
                    rel="noreferrer"
                    class={s.previewLink}
                >
                    Pratinjau Block Page ↗
                </a>
            </div>

            <Show
                when={requests().length > 0}
                fallback={
                    <div class={s.emptyState}>
                        {loading()
                            ? 'Memuat data permohonan...'
                            : 'Belum ada permohonan buka blokir yang diajukan oleh pengguna.'}
                    </div>
                }
            >
                <div class={s.requestList}>
                    <For each={requests()}>
                        {(req) => (
                            <div class={s.requestItem}>
                                <div class={s.itemMain}>
                                    <div class={s.domainHeader}>
                                        <span class={s.domainName}>{req.domain}</span>
                                        <span
                                            class={
                                                req.status === 'approved'
                                                    ? s.statusApproved
                                                    : s.statusPending
                                            }
                                        >
                                            {req.status === 'approved'
                                                ? 'Telah Disetujui'
                                                : 'Menunggu Persetujuan'}
                                        </span>
                                    </div>
                                    <div class={s.metaInfo}>
                                        <span>Pemohon: {req.name || 'Anonim'}</span>
                                        <span>&middot;</span>
                                        <span>IP: {req.client_ip}</span>
                                    </div>
                                    <Show when={req.reason}>
                                        <div class={s.reasonText}>&ldquo;{req.reason}&rdquo;</div>
                                    </Show>
                                </div>
                                <div class={s.actions}>
                                    <Show when={req.status !== 'approved'}>
                                        <button
                                            type="button"
                                            class={s.approveBtn}
                                            onClick={() => handleApprove(req)}
                                        >
                                            Buka Blokir (Whitelist)
                                        </button>
                                    </Show>
                                    <button
                                        type="button"
                                        class={s.rejectBtn}
                                        onClick={() => handleDelete(req.id)}
                                    >
                                        Hapus
                                    </button>
                                </div>
                            </div>
                        )}
                    </For>
                </div>
            </Show>
        </div>
    );
};
