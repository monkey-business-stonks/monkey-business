export interface MarketStatus {
  isOpen: boolean;
  message: string;
}

const newYorkTime = new Intl.DateTimeFormat('en-US', {
  timeZone: 'America/New_York', weekday: 'short',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
});

/** Mirrors OrderValidator.isWithinTradingHours; the server remains authoritative. */
export function getMarketStatus(ticker: string, now = new Date()): MarketStatus {
  const symbol = ticker.toUpperCase();
  if (symbol.startsWith('X:')) {
    return { isOpen: true, message: '' };
  }

  if (symbol.startsWith('FX:')) {
    const parts = newYorkTime.formatToParts(now);
    const part = (type: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === type)?.value;
    const day = part('weekday');
    const minutes = Number(part('hour')) * 60 + Number(part('minute'));
    const isOpen = day === 'Sun' ? minutes >= 17 * 60
      : day === 'Fri' ? minutes < 17 * 60
      : ['Mon', 'Tue', 'Wed', 'Thu'].includes(day ?? '');
    return {
      isOpen,
      message: isOpen ? '' : 'Market closed. Forex buy and sell orders are available Sunday 5:00 PM through Friday 5:00 PM (America/New_York).'
    };
  }

  // Equity hours intentionally use the backend's fixed UTC schedule, not local exchange time.
  const indianEquity = symbol.endsWith('.NS') || symbol.endsWith('.BO');
  const open = indianEquity ? 14 * 60 + 15 : 13 * 60 + 30;
  const close = indianEquity ? 22 * 60 + 30 : 20 * 60;
  const minutes = now.getUTCHours() * 60 + now.getUTCMinutes();
  const weekday = now.getUTCDay() > 0 && now.getUTCDay() < 6;
  const isOpen = !!symbol && weekday && minutes >= open && minutes < close;
  const hours = indianEquity ? '14:15–22:30' : '13:30–20:00';
  return {
    isOpen,
    message: isOpen ? '' : `Market closed. Buy and sell orders are available Monday–Friday, ${hours} UTC.`
  };
}