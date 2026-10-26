import { Asset } from './asset';

export enum AccountType {
  BROKERAGE = 'BROKERAGE',
  TRADITIONAL_401K = 'TRADITIONAL 401K',
  ROTH_401K = 'ROTH 401K',
  TRADITIONAL_IRA = 'TRADITIONAL IRA',
  ROTH_IRA = 'ROTH IRA',
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
  heldAssets?: Asset[];
}
