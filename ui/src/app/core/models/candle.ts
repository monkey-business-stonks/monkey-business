export interface Candle {
    data: CandleData;
    meta: CandleMeta;
}

interface CandleData {
    symbol: string;
    interval: string;
    currency: string;
    candles: CandleEntry[];
}

interface CandleEntry {
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    adjclose: number;
    volume: number;
    synthetic: boolean;
}

interface CandleMeta {
    asOf: string;
    disclaimer: string;
    symbol: string;
    source: string;
    stale: boolean;
    partial: boolean;
    availableFrom: string;
}
