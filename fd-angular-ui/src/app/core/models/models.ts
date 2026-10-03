export interface User {
  id?: number;
  username: string;
  email: string;
  roles: string[];
}

export interface FdAccount {
  fdAccountNo: string;
  customerId: string;
  productCode: string;
  principalAmount: number;
  currentBalance: number;
  accruedInterest: number;
  tenureMonths: number;
  currency: string;
  status: 'ACTIVE' | 'CLOSED' | 'PREMATURE_CLOSED' | 'RENEWED';
  startDate: string;
  maturityDate: string;
  createdAt: string;
  interestRate: number;
  compoundingFrequency: 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY';
  payoutFrequency: 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY' | 'MATURITY';
  maturityInstruction: 'PAYOUT' | 'RENEW_PRINCIPAL' | 'RENEW_PRINCIPAL_AND_INTEREST';
  lastAccrualDate?: string;
  lastCapitalizationDate?: string;
  nextCapitalizationDate?: string;
  lastPayoutDate?: string;
  nextPayoutDate?: string;
  renewalAccountNo?: string;
}

export interface Product {
  productCode: string;
  productName: string;
  productType: string;
  currency: string;
  minRate: number;
  maxRate: number;
  minDeposit: number;
  minTermMonths: number;
  maxTermMonths: number;
  compoundingFrequency: 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY';
  allowedCompoundingFrequencies: Array<'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY'>;
  allowedPayoutFrequencies: Array<'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY' | 'MATURITY'>;
  dayCountConvention: string;
  prematureClosureAllowed: boolean;
}

export interface Transaction {
  txnId: number;
  fdAccountNo: string;
  amount: number;
  txnType: string;
  txnTimestamp: string;
  remarks: string;
}

export interface Statement {
  statementDate: string;
  openingBalance: number;
  interestAccrued: number;
  interestCapitalized: number;
  interestPaid: number;
  accruedInterest: number;
  closingBalance: number;
}

export interface FdSimulationRequest {
  principal: number;
  termMonths: number;
  baseRate: number;
  compoundingFrequency: string;
  calculationType: string;
  categories: string[];
}

export interface FdSimulationResponse {
  maturityAmount: number;
  interestEarned: number;
  effectiveRate: number;
  categoryAddons: any;
}
