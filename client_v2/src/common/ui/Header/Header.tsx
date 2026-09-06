import { createSignal, Show } from 'solid-js';
import { useLocation } from '@solidjs/router';
import cn from 'clsx';
import { Icon } from 'panel/common/ui/Icon';
import { Link } from 'panel/common/ui/Link';
import { Menu } from 'panel/common/ui/Menu';
import { Dropdown } from 'panel/common/ui/Dropdown';
import { Paths, RoutePath } from 'panel/components/Routes/Paths';
import intl from 'panel/common/intl';
import { getLogoutUrl } from 'panel/api/generated';
import { dashboardState } from 'panel/stores/dashboard';

import s from './Header.module.pcss';
import { Logo } from '../Sidebar/Logo';

const BURGER_MENU_ID = 'linksMenu';

export const Header = () => {
    const location = useLocation();
    const [burgerModal, setBurgerModal] = createSignal(false);
    const [accountSubMenu, setAccountSubMenu] = createSignal(false);
    const [settingsOpen, setSettingsOpen] = createSignal(false);
    const [filtersOpen, setFiltersOpen] = createSignal(false);

    const isActive = (path: string | string[], full = false) => {
        const currentPath = location.pathname;
        const paths = Array.isArray(path) ? path : [path];

        return paths.some((p) => {
            if (currentPath === p) {
                return true;
            }

            if (full) {
                return false;
            }

            const normalizedPath = p.endsWith('/') ? p : `${p}/`;
            return currentPath.startsWith(normalizedPath);
        });
    };

    const isSettingsActive = () =>
        [
            Paths.SettingsPage,
            Paths.Dns,
            Paths.DnsPrivateReverse,
            Paths.Encryption,
            Paths.Clients,
            Paths.ClientsAdd,
            Paths.ClientsEdit,
            Paths.ClientsProtection,
            Paths.ClientsEditProtection,
            Paths.ClientsSchedule,
            Paths.ClientsEditSchedule,
            Paths.ClientsBlockedServices,
            Paths.ClientsEditBlockedServices,
            Paths.Dhcp,
            Paths.DhcpLeases,
        ].some((path) => isActive(path));

    const isFiltersActive = () =>
        [
            Paths.DnsBlocklists,
            Paths.DnsAllowlists,
            Paths.DnsRewrites,
            Paths.BlockedServices,
            Paths.CustomRules,
            Paths.InactivitySchedule,
        ].some((path) => isActive(path));

    const closeBurgerMenu = (event: any) => {
        const target = event.target as HTMLDivElement;

        if (!target.closest(`#${BURGER_MENU_ID}`)) {
            setBurgerModal(false);
            document.body.classList.remove('block-scroll');
        }
    };

    const closeSubMenu = () => {
        document.body.classList.remove('block-scroll');
        setBurgerModal(false);
        setAccountSubMenu(false);
    };

    const openBurgerMenu = () => {
        document.body.classList.add('block-scroll');
        setBurgerModal(true);
    };

    return (
        <header class={s.header} id="header">
            <div class={s.container}>
                {/* ── Left: Burger Button & Logo ── */}
                <div class={s.logoWrap}>
                    <Icon onClick={openBurgerMenu} class={s.burgerIcon} icon="butter" />
                    <Link to={RoutePath.Dashboard} class={s.link}>
                        <div class={s.linkWrapper}>
                            <Logo id="header" />
                        </div>
                    </Link>
                </div>

                {/* ── Center: Desktop Horizontal Top Navigation ── */}
                <nav class={s.desktopNav} aria-label="Main Navigation">
                    {/* Dashboard */}
                    <Link
                        class={cn(s.navLink, {
                            [s.activeLink]: isActive(Paths.Dashboard, true),
                        })}
                        to={RoutePath.Dashboard}
                    >
                        <Icon class={s.navIcon} icon="dashboard" />
                        <span>{intl.getMessage('dashboard')}</span>
                    </Link>

                    {/* Settings Dropdown */}
                    <Dropdown
                        open={settingsOpen()}
                        onOpenChange={setSettingsOpen}
                        position="bottomLeft"
                        wrapClass={s.dropdownWrap}
                        overlayClass={s.dropdownOverlay}
                        noIcon
                        menu={
                            <div class={s.dropdownMenu}>
                                <Link
                                    to={RoutePath.SettingsPage}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.SettingsPage),
                                    })}
                                    onClick={() => setSettingsOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="settings" />
                                    <span>{intl.getMessage('settings_general_short')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.Dns}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.Dns),
                                    })}
                                    onClick={() => setSettingsOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="settings_info" />
                                    <span>DNS</span>
                                </Link>
                                <Link
                                    to={RoutePath.Encryption}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.Encryption),
                                    })}
                                    onClick={() => setSettingsOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="lock" />
                                    <span>{intl.getMessage('protocols')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.Clients}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.Clients),
                                    })}
                                    onClick={() => setSettingsOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="user" />
                                    <span>{intl.getMessage('clients')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.Dhcp}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.Dhcp),
                                    })}
                                    onClick={() => setSettingsOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="router" />
                                    <span>DHCP</span>
                                </Link>
                            </div>
                        }
                    >
                        <button
                            type="button"
                            class={cn(s.navLink, s.dropdownTrigger, {
                                [s.activeLink]: isSettingsActive(),
                                [s.openLink]: settingsOpen(),
                            })}
                        >
                            <Icon class={s.navIcon} icon="settings" />
                            <span>{intl.getMessage('settings')}</span>
                            <Icon
                                class={cn(s.arrowIcon, { [s.arrowIconOpen]: settingsOpen() })}
                                icon="arrow_bottom"
                            />
                        </button>
                    </Dropdown>

                    {/* Filters Dropdown */}
                    <Dropdown
                        open={filtersOpen()}
                        onOpenChange={setFiltersOpen}
                        position="bottomLeft"
                        wrapClass={s.dropdownWrap}
                        overlayClass={s.dropdownOverlay}
                        noIcon
                        menu={
                            <div class={s.dropdownMenu}>
                                <Link
                                    to={RoutePath.DnsBlocklists}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.DnsBlocklists),
                                    })}
                                    onClick={() => setFiltersOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="bullets" />
                                    <span>{intl.getMessage('blocklists_title')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.DnsAllowlists}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.DnsAllowlists),
                                    })}
                                    onClick={() => setFiltersOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="check" />
                                    <span>{intl.getMessage('allowlists')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.DnsRewrites}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.DnsRewrites),
                                    })}
                                    onClick={() => setFiltersOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="refresh" />
                                    <span>{intl.getMessage('dns_rewrites')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.BlockedServices}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.BlockedServices),
                                    })}
                                    onClick={() => setFiltersOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="cross" />
                                    <span>{intl.getMessage('blocked_services')}</span>
                                </Link>
                                <Link
                                    to={RoutePath.CustomRules}
                                    class={cn(s.dropdownItem, {
                                        [s.activeDropdownItem]: isActive(Paths.CustomRules),
                                    })}
                                    onClick={() => setFiltersOpen(false)}
                                >
                                    <Icon class={s.dropdownItemIcon} icon="edit" />
                                    <span>{intl.getMessage('user_rules_title')}</span>
                                </Link>
                            </div>
                        }
                    >
                        <button
                            type="button"
                            class={cn(s.navLink, s.dropdownTrigger, {
                                [s.activeLink]: isFiltersActive(),
                                [s.openLink]: filtersOpen(),
                            })}
                        >
                            <Icon class={s.navIcon} icon="tune" />
                            <span>{intl.getMessage('filters')}</span>
                            <Icon
                                class={cn(s.arrowIcon, { [s.arrowIconOpen]: filtersOpen() })}
                                icon="arrow_bottom"
                            />
                        </button>
                    </Dropdown>

                    {/* Query Log */}
                    <Link
                        class={cn(s.navLink, {
                            [s.activeLink]: isActive(Paths.Logs),
                        })}
                        to={RoutePath.Logs}
                    >
                        <Icon class={s.navIcon} icon="log" />
                        <span>{intl.getMessage('logs')}</span>
                    </Link>

                    {/* Setup Guide */}
                    <Link
                        class={cn(s.navLink, {
                            [s.activeLink]: isActive(Paths.Guide),
                        })}
                        to={RoutePath.Guide}
                    >
                        <Icon class={s.navIcon} icon="faq" />
                        <span>{intl.getMessage('setup_guide')}</span>
                    </Link>
                </nav>

                {/* ── Right Section: Status Badge & Logout ── */}
                <div class={s.rightSection}>
                    <Show when={!dashboardState.processing && dashboardState.isCoreRunning}>
                        <div
                            class={cn(s.statusBadge, {
                                [s.statusProtected]: dashboardState.protectionEnabled,
                            })}
                            title={
                                dashboardState.protectionEnabled
                                    ? intl.getMessage('protection_enabled')
                                    : intl.getMessage('protection_disabled')
                            }
                        >
                            <span class={s.statusDot} />
                            <span class={s.statusLabel}>
                                {dashboardState.protectionEnabled
                                    ? intl.getMessage('enabled')
                                    : intl.getMessage('disabled')}
                            </span>
                        </div>
                    </Show>

                    <a
                        href={getLogoutUrl()}
                        target="_blank"
                        rel="noopener noreferrer"
                        class={s.logoutBtn}
                        id="sign_out"
                        title={intl.getMessage('logout')}
                    >
                        <Icon class={s.logoutIcon} icon="logout" />
                        <span class={s.logoutLabel}>{intl.getMessage('logout')}</span>
                    </a>
                </div>
            </div>

            {/* ── Mobile Burger Drawer ── */}
            <div
                class={cn(s.burgerMenuMask, { [s.open]: burgerModal() })}
                onClick={(event: any) => closeBurgerMenu(event)}
            >
                <div class={s.burgerMenu}>
                    <Menu
                        headerMenu
                        accountSubMenu={accountSubMenu()}
                        setAccountSubMenu={setAccountSubMenu}
                        burgerMenuId={BURGER_MENU_ID}
                        closeSubMenu={closeSubMenu}
                    />
                </div>
            </div>
        </header>
    );
};
