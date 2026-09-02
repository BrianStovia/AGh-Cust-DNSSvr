import { brandingState } from 'panel/stores/branding';
import s from './styles.module.pcss';

type Props = {
    id?: string;
    class?: string;
};

export const Logo = (props: Props) => {
    const title = () => brandingState.title || 'DNS SERVER';
    const subtitle = () => brandingState.subtitle || 'BRST';
    const primary = () => brandingState.primaryColor || '#0d84f8';
    const accent = () => brandingState.accentColor || '#00d4ef';
    const glow = () => brandingState.glowColor || '0 0 16px rgba(13, 132, 248, 0.45)';

    return (
        <div
            style={{
                display: 'flex',
                'align-items': 'center',
                gap: '10px',
            }}
            class={props.class}
        >
            <div
                style={{
                    display: 'flex',
                    'align-items': 'center',
                    'justify-content': 'center',
                    width: '36px',
                    height: '36px',
                    'border-radius': '10px',
                    background: `linear-gradient(135deg, ${primary()} 0%, ${accent()} 100%)`,
                    'box-shadow': glow(),
                    'flex-shrink': '0',
                    transition: 'all 0.3s ease',
                }}
            >
                <img
                    src="assets/logo.svg"
                    alt={`${title()} ${subtitle()} Logo`}
                    style={{
                        height: '22px',
                        width: 'auto',
                        'max-width': '26px',
                        'object-fit': 'contain',
                        filter: 'brightness(0) invert(1)',
                    }}
                />
            </div>
            <div style={{ display: 'flex', 'flex-direction': 'column', 'line-height': '1' }}>
                <span
                    style={{
                        'font-weight': '700',
                        'font-size': '13px',
                        'letter-spacing': '1.5px',
                        color: '#ffffff',
                        'text-transform': 'uppercase',
                        'white-space': 'nowrap',
                    }}
                >
                    {title()}
                </span>
                <span
                    style={{
                        'font-weight': '800',
                        'font-size': '15px',
                        'letter-spacing': '2px',
                        background: `linear-gradient(90deg, ${primary()}, ${accent()})`,
                        '-webkit-background-clip': 'text',
                        '-webkit-text-fill-color': 'transparent',
                        'background-clip': 'text',
                        'white-space': 'nowrap',
                        'margin-top': '1px',
                    }}
                >
                    {subtitle()}
                </span>
            </div>
        </div>
    );
};
