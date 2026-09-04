import { createSignal, createMemo, Show } from 'solid-js';
import cn from 'clsx';
import { dashboardState } from 'panel/stores/dashboard';
import { addSuccessToast, addErrorToast } from 'panel/stores/toasts';
import { Button } from 'panel/common/ui/Button';
import { changePassword } from 'panel/api/generated';
import s from './styles.module.pcss';

export const AccountConfig = () => {
    const [currentPassword, setCurrentPassword] = createSignal('');
    const [newPassword, setNewPassword] = createSignal('');
    const [confirmPassword, setConfirmPassword] = createSignal('');

    const [showCurrent, setShowCurrent] = createSignal(false);
    const [showNew, setShowNew] = createSignal(false);
    const [showConfirm, setShowConfirm] = createSignal(false);

    const [processing, setProcessing] = createSignal(false);
    const [errorMsg, setErrorMsg] = createSignal('');
    const [successMsg, setSuccessMsg] = createSignal('');

    const username = createMemo(() => dashboardState.name || 'admin');

    const passwordStrength = createMemo(() => {
        const pass = newPassword();
        if (!pass) return { score: 0, label: '', class: '' };

        let score = 0;
        if (pass.length >= 4) score += 1;
        if (pass.length >= 8) score += 1;
        if (/[A-Z]/.test(pass) && /[a-z]/.test(pass)) score += 1;
        if (/[0-9]/.test(pass)) score += 1;
        if (/[^A-Za-z0-9]/.test(pass)) score += 1;

        if (score <= 2) {
            return { score: 1, label: 'Lemah', class: s.weak };
        } else if (score <= 3) {
            return { score: 2, label: 'Sedang', class: s.medium };
        } else {
            return { score: 3, label: 'Kuat', class: s.strong };
        }
    });

    const isMatch = createMemo(() => {
        if (!confirmPassword()) return true;
        return newPassword() === confirmPassword();
    });

    const handleReset = () => {
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setErrorMsg('');
        setSuccessMsg('');
    };

    const handleSubmit = async (e: Event) => {
        e.preventDefault();
        setErrorMsg('');
        setSuccessMsg('');

        const curr = currentPassword().trim();
        const next = newPassword();
        const conf = confirmPassword();

        if (!curr) {
            setErrorMsg('Kata sandi saat ini harus diisi.');
            return;
        }

        if (next.length < 4) {
            setErrorMsg('Kata sandi baru minimal harus 4 karakter.');
            return;
        }

        if (next !== conf) {
            setErrorMsg('Konfirmasi kata sandi tidak cocok dengan kata sandi baru.');
            return;
        }

        setProcessing(true);

        try {
            await changePassword({
                current_password: curr,
                new_password: next,
            });

            setSuccessMsg('Kata sandi administrator berhasil diperbarui.');
            addSuccessToast('Kata sandi administrator berhasil diubah!');
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (err: any) {
            let message = 'Gagal mengubah kata sandi. Periksa kata sandi lama Anda.';
            if (err && err.message) {
                if (err.message.includes('invalid current password')) {
                    message = 'Kata sandi saat ini salah!';
                } else if (err.message.includes('too short') || err.message.includes('at least 4')) {
                    message = 'Kata sandi baru minimal 4 karakter.';
                } else if (err.message.includes('not supported')) {
                    message = 'Penggantian kata sandi tidak didukung pada mode ini.';
                }
            }
            setErrorMsg(message);
            addErrorToast({ error: message });
        } finally {
            setProcessing(false);
        }
    };

    // Eye SVGs
    const EyeIcon = () => (
        <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
        >
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
        </svg>
    );

    const EyeOffIcon = () => (
        <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
        >
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
            <line x1="1" y1="1" x2="23" y2="23" />
        </svg>
    );

    return (
        <div class={s.accountContainer} id="account-security">
            <div class={s.accountHeader}>
                <div class={s.titleArea}>
                    <span class={s.badge}>Keamanan</span>
                    <h2 class={s.title}>Keamanan Akun &amp; Ganti Password</h2>
                </div>
            </div>

            <p class={s.desc}>
                Perbarui kata sandi akun administrator untuk mengamankan akses ke dashboard AdGuard Home DNS Server Anda.
            </p>

            {/* ── Active User Info Banner ── */}
            <div class={s.userBanner}>
                <div class={s.userInfo}>
                    <div class={s.userAvatar}>
                        {username().charAt(0).toUpperCase()}
                    </div>
                    <div class={s.userMeta}>
                        <span class={s.userName}>{username()}</span>
                        <span class={s.userRole}>Akun Administrator Aktif</span>
                    </div>
                </div>
                <span class={s.roleTag}>Super Admin</span>
            </div>

            {/* ── Status Alerts ── */}
            <Show when={errorMsg()}>
                <div class={cn(s.alertBox, s.alertError)}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    <span>{errorMsg()}</span>
                </div>
            </Show>

            <Show when={successMsg()}>
                <div class={cn(s.alertBox, s.alertSuccess)}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                        <polyline points="22 4 12 14.01 9 11.01" />
                    </svg>
                    <span>{successMsg()}</span>
                </div>
            </Show>

            {/* ── Change Password Form ── */}
            <form onSubmit={handleSubmit}>
                <div class={s.formGrid}>
                    {/* Current Password */}
                    <div class={s.fieldGroup}>
                        <label class={s.label} for="current_password">
                            Kata Sandi Saat Ini
                        </label>
                        <div class={s.inputWrapper}>
                            <input
                                id="current_password"
                                type={showCurrent() ? 'text' : 'password'}
                                class={s.input}
                                placeholder="Masukkan kata sandi lama"
                                value={currentPassword()}
                                onInput={(e) => setCurrentPassword(e.currentTarget.value)}
                                autocomplete="current-password"
                                required
                            />
                            <button
                                type="button"
                                class={s.toggleBtn}
                                onClick={() => setShowCurrent(!showCurrent())}
                                title={showCurrent() ? 'Sembunyikan' : 'Tampilkan'}
                            >
                                <Show when={showCurrent()} fallback={<EyeIcon />}>
                                    <EyeOffIcon />
                                </Show>
                            </button>
                        </div>
                    </div>

                    {/* New Password */}
                    <div class={s.fieldGroup}>
                        <label class={s.label} for="new_password">
                            <span>Kata Sandi Baru</span>
                            <Show when={newPassword()}>
                                <span class={cn(s.strengthLabel, passwordStrength().class)}>
                                    {passwordStrength().label}
                                </span>
                            </Show>
                        </label>
                        <div class={s.inputWrapper}>
                            <input
                                id="new_password"
                                type={showNew() ? 'text' : 'password'}
                                class={s.input}
                                placeholder="Minimal 4 karakter"
                                value={newPassword()}
                                onInput={(e) => setNewPassword(e.currentTarget.value)}
                                autocomplete="new-password"
                                required
                            />
                            <button
                                type="button"
                                class={s.toggleBtn}
                                onClick={() => setShowNew(!showNew())}
                                title={showNew() ? 'Sembunyikan' : 'Tampilkan'}
                            >
                                <Show when={showNew()} fallback={<EyeIcon />}>
                                    <EyeOffIcon />
                                </Show>
                            </button>
                        </div>
                        {/* Strength Indicator Bars */}
                        <Show when={newPassword()}>
                            <div class={s.strengthMeter}>
                                <div class={s.strengthBars}>
                                    <div
                                        class={cn(s.strengthBar, {
                                            [passwordStrength().class]: passwordStrength().score >= 1,
                                        })}
                                    />
                                    <div
                                        class={cn(s.strengthBar, {
                                            [passwordStrength().class]: passwordStrength().score >= 2,
                                        })}
                                    />
                                    <div
                                        class={cn(s.strengthBar, {
                                            [passwordStrength().class]: passwordStrength().score >= 3,
                                        })}
                                    />
                                </div>
                            </div>
                        </Show>
                    </div>

                    {/* Confirm Password */}
                    <div class={s.fieldGroup}>
                        <label class={s.label} for="confirm_password">
                            Konfirmasi Kata Sandi Baru
                        </label>
                        <div class={s.inputWrapper}>
                            <input
                                id="confirm_password"
                                type={showConfirm() ? 'text' : 'password'}
                                class={cn(s.input, { [s.inputError]: !isMatch() })}
                                placeholder="Ketik ulang kata sandi baru"
                                value={confirmPassword()}
                                onInput={(e) => setConfirmPassword(e.currentTarget.value)}
                                autocomplete="new-password"
                                required
                            />
                            <button
                                type="button"
                                class={s.toggleBtn}
                                onClick={() => setShowConfirm(!showConfirm())}
                                title={showConfirm() ? 'Sembunyikan' : 'Tampilkan'}
                            >
                                <Show when={showConfirm()} fallback={<EyeIcon />}>
                                    <EyeOffIcon />
                                </Show>
                            </button>
                        </div>
                        <Show when={!isMatch()}>
                            <span class={s.errorMsg}>Konfirmasi kata sandi tidak cocok</span>
                        </Show>
                    </div>
                </div>

                {/* ── Form Actions ── */}
                <div class={s.actions}>
                    <Button
                        type="submit"
                        variant="primary"
                        class={s.submitBtn}
                        disabled={
                            processing() ||
                            !currentPassword() ||
                            newPassword().length < 4 ||
                            newPassword() !== confirmPassword()
                        }
                    >
                        {processing() ? 'Menyimpan...' : 'Simpan Password Baru'}
                    </Button>
                    <Button
                        type="button"
                        variant="secondary"
                        onClick={handleReset}
                        disabled={processing() || (!currentPassword() && !newPassword() && !confirmPassword())}
                    >
                        Reset
                    </Button>
                </div>
            </form>
        </div>
    );
};
