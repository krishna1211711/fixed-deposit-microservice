export interface User {
  id?: number;
  username: string;
  email: string;
  roles: string[];
  customerId?: string;
}

export interface FdAccount {
  fdAccountNo: string;
  customerId: string;
  customerName?: string;
  customerCategory?: string;
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
  prematureClosureAllowed?: boolean;
  prematureClosurePenaltyPct?: number;
  closureDate?: string;
  closureType?: 'MATURITY' | 'PREMATURE' | 'RENEWAL';
  closureReason?: string;
  closureGrossInterest?: number;
  closurePenaltyAmount?: number;
  closureNetPayout?: number;
  closureTransferAccountMasked?: string;
  closedBy?: string;
}

export interface Product {
  productCode: string;
  productName: string;
  productType: string;
  currency: string;
  minRate: number;
  maxRate: number;
  minDeposit: number;
  maxDeposit?: number;
  minTermMonths: number;
  maxTermMonths: number;
  effectiveDate?: string;
  rateCapAddon?: number;
  preMaturityPenaltyPct?: number;
  status?: string;
  compoundingFrequency: 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY';
  allowedCompoundingFrequencies: Array<'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY'>;
  allowedPayoutFrequencies: Array<'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY' | 'MATURITY'>;
  dayCountConvention: string;
  prematureClosureAllowed: boolean;
  categoryAddonsStackable: boolean;
}

export interface FdOpeningRequest {
  requestId: string;
  requesterUsername: string;
  requesterRole: string;
  customerId: string;
  customerName: string;
  productCode: string;
  principalAmount: number;
  currency: string;
  status: 'PENDING_CHECKER' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  checkerUsername?: string;
  decisionReason?: string;
  fdAccountNo?: string;
  createdAt: string;
  decidedAt?: string;
}

export interface AuditLog {
  auditId: string;
  occurredAt: string;
  actorUsername: string;
  actorRole: string;
  action: string;
  entityType: string;
  entityId?: string;
  outcome: string;
  correlationId?: string;
  detailsJson?: string;
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
  productCode: string;
  principal: number;
  termMonths: number;
  compoundingFrequency: string;
}

export interface FdSimulationResponse {
  maturityAmount: number;
  interestEarned: number;
  effectiveRate: number;
  categoryAddons: any;
}
