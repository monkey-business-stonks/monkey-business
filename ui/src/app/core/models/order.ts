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
  Pending = "PENDING", 
  Succeeded = "SUCCEEDED", 
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
  executedValue: number;
  status: OrderStatus;
  submittedOn: Date;
  executedOn: Date;
  createdOn: Date;
}