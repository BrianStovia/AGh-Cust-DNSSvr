import { createSignal, Show } from 'solid-js';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import { Button } from 'panel/common/ui/Button';
import { customFetch } from 'panel/api/customFetch';
import s from './styles.module.pcss';

interface MaintenanceResult {
    status: string;
    freed_memory_mb: number;
    allocated_mb: number;
    sys_memory_mb: number;
    num_gc: number;
    timestamp: string;
}

export const MaintenanceConfig = () => {
    const [processing, setProcessing] = createSignal(false);
    const [result, setResult] = createSignal<MaintenanceResult | null>(null);

    const handleOptimize = async () => {
        setProcessing(true);
        try {
            const data = await customFetch<MaintenanceResult>('control/maintenance/optimize', {
                method: 'POST',
            });
            setResult(data);
            addSuccessToast(
                `Optimasi berhasil! Membebaskan ${data.freed_memory_mb || 0} MB memori sistem.`,
            );
        } catch (err: any) {
            addErrorToast({ error: 'Gagal melakukan pemeliharaan database.' });
        } finally {
            setProcessing(false);
        }
    };

    return (
        <div class={s.maintenanceContainer} id="maintenance">
            <div class={s.header}>
                <div class={s.titleArea}>
                    <span class={s.badge}>Pemeliharaan</span>
                    <h2 class={s.title}>Auto-Maintenance &amp; Database Optimizer</h2>
                </div>
            </div>

            <p class={s.desc}>
                Pembersihan otomatis cache memori, pemadatan riwayat kueri, dan pengembalian alokasi RAM yang tidak terpakai kembali ke sistem operasi agar server selalu berjalan ringan dan responsif.
            </p>

            <div class={s.autoBanner}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
                </svg>
                <span>Pemeliharaan terjadwal aktif otomatis setiap 24 jam / tengah malam.</span>
            </div>

            <Show when={result()}>
                <div class={s.statsGrid}>
                    <div class={s.statCard}>
                        <span class={s.statLabel}>Memori Dibebaskan</span>
                        <span class={s.statValue}>{result()?.freed_memory_mb} MB</span>
                    </div>
                    <div class={s.statCard}>
                        <span class={s.statLabel}>RAM Heap Terpakai</span>
                        <span class={s.statValue}>{result()?.allocated_mb} MB</span>
                    </div>
                    <div class={s.statCard}>
                        <span class={s.statLabel}>Total Virtual RAM</span>
                        <span class={s.statValue}>{result()?.sys_memory_mb} MB</span>
                    </div>
                    <div class={s.statCard}>
                        <span class={s.statLabel}>Siklus Garbage Collector</span>
                        <span class={s.statValue}>{result()?.num_gc} kali</span>
                    </div>
                </div>
            </Show>

            <div class={s.actions}>
                <Button
                    type="button"
                    variant="primary"
                    onClick={handleOptimize}
                    disabled={processing()}
                >
                    {processing() ? 'Mengoptimalkan...' : 'Optimalkan Sekarang (One-Click Clean)'}
                </Button>
            </div>
        </div>
    );
};
