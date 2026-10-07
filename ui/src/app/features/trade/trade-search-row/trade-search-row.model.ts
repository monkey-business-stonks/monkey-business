import { Asset, Quote } from '@/app/core/models';

export interface TradeSearchRowData {
  asset: Asset;
  quote: Quote;
  selected: boolean;
}
