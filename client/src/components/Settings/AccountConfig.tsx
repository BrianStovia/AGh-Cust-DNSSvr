import React, { useState } from 'react';
import Card from '../ui/Card';

export const AccountConfig: React.FC = () => {
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showCurrent, setShowCurrent] = useState(false);
    const [showNew, setShowNew] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [processing, setProcessing] = useState(false);
    const [statusMsg, setStatusMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setStatusMsg(null);

        if (!currentPassword) {
            setStatusMsg({ type: 'error', text: 'Kata sandi saat ini harus diisi.' });
            return;
        }

        if (newPassword.length < 4) {
            setStatusMsg({ type: 'error', text: 'Kata sandi baru minimal 4 karakter.' });
            return;
        }

        if (newPassword !== confirmPassword) {
            setStatusMsg({ type: 'error', text: 'Konfirmasi kata sandi tidak cocok.' });
            return;
        }

        setProcessing(true);

        try {
            const res = await fetch('control/profile/change_password', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    current_password: currentPassword,
                    new_password: newPassword,
                }),
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || 'Gagal mengubah kata sandi');
            }

            setStatusMsg({ type: 'success', text: 'Kata sandi administrator berhasil diperbarui!' });
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (err: any) {
            let msg = 'Gagal mengubah kata sandi. Periksa kata sandi lama Anda.';
            if (err && err.message) {
                if (err.message.includes('invalid current password')) {
                    msg = 'Kata sandi saat ini salah!';
                } else if (err.message.includes('at least 4') || err.message.includes('too short')) {
                    msg = 'Kata sandi baru minimal 4 karakter.';
                }
            }
            setStatusMsg({ type: 'error', text: msg });
        } finally {
            setProcessing(false);
        }
    };

    return (
        <Card
            title="Keamanan Akun & Ganti Password"
            subtitle="Perbarui kata sandi akun administrator untuk mengamankan akses ke dashboard DNS Server"
            bodyType="card-body box-body--settings"
        >
            <form onSubmit={handleSubmit} className="form" style={{ maxWidth: '640px' }}>
                {statusMsg && (
                    <div
                        className={`alert alert-${statusMsg.type === 'success' ? 'success' : 'danger'}`}
                        style={{ marginBottom: '16px' }}
                    >
                        {statusMsg.text}
                    </div>
                )}

                <div className="form__group">
                    <label className="form__label" htmlFor="react_curr_pass">
                        Kata Sandi Saat Ini
                    </label>
                    <div style={{ position: 'relative' }}>
                        <input
                            id="react_curr_pass"
                            type={showCurrent ? 'text' : 'password'}
                            className="form-control"
                            placeholder="Masukkan kata sandi lama"
                            value={currentPassword}
                            onChange={(e) => setCurrentPassword(e.target.value)}
                            required
                        />
                        <button
                            type="button"
                            onClick={() => setShowCurrent(!showCurrent)}
                            style={{
                                position: 'absolute',
                                right: '10px',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                            }}
                        >
                            {showCurrent ? '👁️' : '🙈'}
                        </button>
                    </div>
                </div>

                <div className="form__group">
                    <label className="form__label" htmlFor="react_new_pass">
                        Kata Sandi Baru
                    </label>
                    <div style={{ position: 'relative' }}>
                        <input
                            id="react_new_pass"
                            type={showNew ? 'text' : 'password'}
                            className="form-control"
                            placeholder="Minimal 4 karakter"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            required
                        />
                        <button
                            type="button"
                            onClick={() => setShowNew(!showNew)}
                            style={{
                                position: 'absolute',
                                right: '10px',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                            }}
                        >
                            {showNew ? '👁️' : '🙈'}
                        </button>
                    </div>
                </div>

                <div className="form__group">
                    <label className="form__label" htmlFor="react_conf_pass">
                        Konfirmasi Kata Sandi Baru
                    </label>
                    <div style={{ position: 'relative' }}>
                        <input
                            id="react_conf_pass"
                            type={showConfirm ? 'text' : 'password'}
                            className="form-control"
                            placeholder="Ulangi kata sandi baru"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                        />
                        <button
                            type="button"
                            onClick={() => setShowConfirm(!showConfirm)}
                            style={{
                                position: 'absolute',
                                right: '10px',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                            }}
                        >
                            {showConfirm ? '👁️' : '🙈'}
                        </button>
                    </div>
                </div>

                <div style={{ marginTop: '20px' }}>
                    <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={processing || !currentPassword || newPassword.length < 4 || newPassword !== confirmPassword}
                    >
                        {processing ? 'Menyimpan...' : 'Simpan Password Baru'}
                    </button>
                </div>
            </form>
        </Card>
    );
};

export default AccountConfig;
