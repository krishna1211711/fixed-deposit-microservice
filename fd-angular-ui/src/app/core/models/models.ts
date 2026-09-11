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
  termMonths: number;
  branchCode: string;
  currency: string;
  status: 'ACTIVE' | 'CLOSED' | 'PREMATURE_CLOSED';
  maturityDate: string;
  createdAt: string;
  interestRate: number;
}

export interface Product {
  code: string;
  name: string;
  baseRate: number;
  compoundingFrequency: 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY';
  calculationType: 'SIMPLE' | 'COMPOUND';
}

export interface Transaction {
  transactionId: string;
  fdAccountNo: string;
  amount: number;
  type: string;
  timestamp: string;
  remarks: string;
}

export interface Statement {
  date: string;
  openingBalance: number;
  interestCredited: number;
  closingBalance: number;
}

export interface FdSimulationRequest {
  principalAmount: number;
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
