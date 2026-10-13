export enum AccountType {
  BROKERAGE = 'BROKERAGE',
  _401K = '_401K',
  ROTH_IRA = 'ROTH_IRA',
  CRYPTO = 'CRYPTO',
  FOREX = 'FOREX'
}

export interface Account {
  accountId: string;
  userId: string;
  accountType: string;
  openedDate: Date;
  balance: number;
  cashBalance: number;
}
