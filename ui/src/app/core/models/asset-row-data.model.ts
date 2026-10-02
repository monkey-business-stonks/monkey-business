export interface AssetRowData {
  symbol: string;
  name: string;
  price: number;
  changePercent: number;
  changeAmount: number;
  exchange: string;
  type: string;
  bid: number;
  ask: number;
  spread: number;
  selected?: boolean;
}