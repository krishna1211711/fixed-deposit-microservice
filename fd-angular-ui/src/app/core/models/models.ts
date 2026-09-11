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
  tenureMonths: number;
  currency: string;
  status: 'ACTIVE' | 'CLOSED' | 'PREMATURE_CLOSED';
  maturityDate: string;
  createdAt: string;
  interestRate: number;
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
  compoundingFrequency: 'MONTHLY' | 'QUARTERLY' | 'HALFYEARLY' | 'YEARLY';
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
  interestCredited: number;
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
