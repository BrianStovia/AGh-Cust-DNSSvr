import { Show } from 'solid-js';
import cn from 'clsx';

import { Icon } from 'panel/common/ui/Icon';
import theme from 'panel/lib/theme';
import { getClientLocation } from 'panel/components/QueryLog/helpers';
import { getCountryFlag } from 'panel/helpers/flags';
import type { NormalizedQueryLogItem } from 'panel/helpers/helpers';

import s from '../LogTable.module.pcss';

type Props = {
    onSearchSelect: (value: string) => (event: MouseEvent) => void;
    row: NormalizedQueryLogItem;
};

export const ClientCell = (props: Props) => {
    const clientName = () => props.row.client_info?.name || '';
    const clientLocation = () => getClientLocation(props.row.client_info?.whois);
    const countryFlag = () => getCountryFlag(props.row.client_info?.whois?.country);

    return (
        <div class={s.clientCell} data-testid="query-log-client-cell">
            <div class={s.clientPrimary}>
                <Show when={countryFlag()}>
                    <span class={s.flagBadge} title={props.row.client_info?.whois?.country}>
                        {countryFlag()}
                    </span>
                </Show>
                <button
                    type="button"
                    class={cn(s.clientButtonPlain, s.clientIp, theme.text.t3)}
                    title={props.row.client}
                    onClick={(e) => {
                        e.stopPropagation();
                        props.onSearchSelect(props.row.client)(e);
                    }}
                >
                    {props.row.client}
                </button>
            </div>
            <div class={s.clientSecondary}>
                <Show when={clientName()}>
                    <button
                        type="button"
                        class={cn(s.clientButtonPlain, s.clientName, theme.text.t4)}
                        title={clientName()}
                        onClick={(e) => props.onSearchSelect(clientName())(e)}
                    >
                        {clientName()}
                    </button>
                </Show>

                <Show when={clientName() && clientLocation()}>
                    <span class={s.clientLocationDivider} />
                </Show>

                <Show when={clientLocation()}>
                    <span class={s.clientLocation} title={clientLocation()}>
                        <Show when={!countryFlag()}>
                            <Icon icon="location" class={s.clientLocationIcon} />
                        </Show>
                        <span class={cn(s.clientLocationText, theme.text.t4)}>
                            {clientLocation()}
                        </span>
                    </span>
                </Show>
            </div>
        </div>
    );
};
