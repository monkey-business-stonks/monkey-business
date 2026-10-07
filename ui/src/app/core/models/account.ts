export enum AccountType {
  "BROKERAGE", 
  "401K", 
  "ROTH IRA", 
  "CRYPTO", 
  "FOREX"
}

export interface Account {
  accountId: string;
  userId: string;
  accountType: string;
  openedDate: Date;
  balance: number;
  cashBalance: number;
}
