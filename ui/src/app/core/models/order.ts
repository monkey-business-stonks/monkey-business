export enum OrderType {
  Equity = "EQUITY", 
  ForeignExchange = "FOREIGN EXCHANGE", 
  Crypto = "CRYPTO"
} 

export enum OrderAction {
  Buy = "BUY", 
  Sell = "SELL", 
  Exchange = "EXCHANGE"
} 

export enum OrderStatus {
  Submitted = "SUBMITTED",
  Accepted = "ACCEPTED",
  Filled = "FILLED",
  Settled = "SETTLED",
  Rejected = "REJECTED"
}

export interface Order {
  orderId: string;
  accountId: string;
  orderType: OrderType;
  action: OrderAction;
  ticker: string;
  quantity: number;
  submittedValue: number;
  executedValue: number | null;
  status: OrderStatus;
  submittedOn: Date;
  verifiedOn: Date | null;
  settledOn: Date | null;
  executedOn: Date | null;
  createdOn: Date;
}