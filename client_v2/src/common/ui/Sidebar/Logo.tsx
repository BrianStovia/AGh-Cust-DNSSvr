import s from './styles.module.pcss';

type Props = {
    id?: string;
    class?: string;
};

export const Logo = (props: Props) => {
    return (
        <div style={{ display: 'flex', 'align-items': 'center', gap: '10px' }} class={props.class}>
            <img
                src="assets/logo.svg"
                alt="DNS SERVER BRST Logo"
                style={{ height: '32px', width: 'auto', 'max-width': '40px', 'object-fit': 'contain' }}
            />
            <span style={{ 'font-weight': '700', 'font-size': '16px', 'letter-spacing': '0.5px', color: 'var(--default-logo-color, #2b7afb)', 'white-space': 'nowrap' }}>
                DNS SERVER BRST
            </span>
        </div>
    );
};
