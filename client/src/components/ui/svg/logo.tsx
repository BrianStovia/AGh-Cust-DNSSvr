import React, { memo } from 'react';

type Props = {
    className?: string;
};

export const Logo = memo(({ className }: Props) => {
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }} className={className}>
            <img
                src="assets/logo.svg"
                alt="DNS SERVER BRST Logo"
                style={{ height: '32px', width: 'auto', maxWidth: '40px', objectFit: 'contain' }}
            />
            <span style={{ fontWeight: '700', fontSize: '16px', letterSpacing: '0.5px', color: '#2b7afb', whiteSpace: 'nowrap' }}>
                DNS SERVER BRST
            </span>
        </div>
    );
});

Logo.displayName = 'Logo';
