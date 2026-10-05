/**
 * Account Types
 * Matches the AccountType enum from the backend
 */
export type AccountType = 'BROKERAGE' | '_401K' | 'ROTH_IRA' | 'CRYPTO' | 'FOREX';

/**
 * Account Creation Request
 * Data structure for creating a new investment account
 */
export interface CreateAccountRequest {
  accountType: AccountType;
  accountName: string;
  initialBalance?: number;
}

/**
 * Account Response
 * Data structure returned from backend after account creation
 */
export interface AccountResponse {
  accountId: string;
  accountType: AccountType;
  accountName: string;
  balance: number;
  cashBalance: number;
  createdOn: string;
  updatedAt: string;
}

/**
 * Account Display
 * Account information for UI display
 */
export interface Account extends AccountResponse {
  accountDisplayName: string;
}
