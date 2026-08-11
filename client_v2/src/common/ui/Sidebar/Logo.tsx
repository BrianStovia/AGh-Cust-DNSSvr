import s from './styles.module.pcss';

type Props = {
    id?: string;
    class?: string;
};

export const Logo = (props: Props) => {
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
                    background: 'linear-gradient(135deg, #0d84f8 0%, #00d4ef 100%)',
                    'box-shadow': '0 0 16px rgba(13, 132, 248, 0.45)',
                    'flex-shrink': '0',
                }}
            >
                <img
                    src="assets/logo.svg"
                    alt="DNS SERVER BRST Logo"
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
                    DNS SERVER
                </span>
                <span
                    style={{
                        'font-weight': '800',
                        'font-size': '15px',
                        'letter-spacing': '2px',
                        'background': 'linear-gradient(90deg, #0d84f8, #00d4ef)',
                        '-webkit-background-clip': 'text',
                        '-webkit-text-fill-color': 'transparent',
                        'background-clip': 'text',
                        'white-space': 'nowrap',
                        'margin-top': '1px',
                    }}
                >
                    BRST
                </span>
            </div>
        </div>
    );
};
