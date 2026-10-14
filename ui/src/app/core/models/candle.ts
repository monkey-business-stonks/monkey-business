export interface Candle {
    data: CandleData;
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
