export interface Data {
  symbol: string;
  price: number;
  bid: number;
  ask: number;
  spreadBps: number;
  currency: string;
  change: number;
  changePercent: number;
  previousClose: number;
  asOf: string;
  marketState: string;
}