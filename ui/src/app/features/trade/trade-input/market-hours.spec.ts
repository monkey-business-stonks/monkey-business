import { getMarketStatus } from './market-hours';

describe('market hours matching the backend', () => {
  it.each([
    ['AAPL', '2026-10-21T13:29:59Z', false],
    ['AAPL', '2026-10-21T13:30:00Z', true],
    ['AAPL', '2026-10-21T19:59:59Z', true],
    ['AAPL', '2026-10-21T20:00:00Z', false],
    ['AAPL', '2026-10-24T16:00:00Z', false],
    ['AAPL', '2026-10-25T16:00:00Z', false],
    ['AAPL', '2026-01-21T13:30:00Z', true],
    ['INFY.NS', '2026-10-21T14:14:59Z', false],
    ['INFY.NS', '2026-10-21T14:15:00Z', true],
    ['INFY.NS', '2026-10-21T22:29:59Z', true],
    ['INFY.NS', '2026-10-21T22:30:00Z', false],
    ['infy.bo', '2026-10-21T14:15:00Z', true],
    ['INFY.BO', '2026-10-24T16:00:00Z', false],
    ['FX:EURUSD', '2026-07-05T20:59:59Z', false],
    ['FX:EURUSD', '2026-07-05T21:00:00Z', true],
    ['FX:EURUSD', '2026-07-10T20:59:59Z', true],
    ['FX:EURUSD', '2026-07-10T21:00:00Z', false],
    ['FX:EURUSD', '2026-01-04T21:59:59Z', false],
    ['FX:EURUSD', '2026-01-04T22:00:00Z', true],
    ['FX:EURUSD', '2026-01-09T21:59:59Z', true],
    ['FX:EURUSD', '2026-01-09T22:00:00Z', false],
    ['FX:EURUSD', '2026-01-10T12:00:00Z', false],
    ['FX:EURUSD', '2026-01-07T05:00:00Z', true],
    ['X:BTC-USD', '2026-10-24T00:00:00Z', true],
    ['X:BTC-USD', '2026-10-25T23:59:59Z', true],
    ['', '2026-10-21T16:00:00Z', false]
  ])('%s at %s is open: %s', (ticker, time, expected) => {
    const status = getMarketStatus(ticker, new Date(time));
    expect(status.isOpen).toBe(expected);
    expect(status.message === '').toBe(expected);
  });
});