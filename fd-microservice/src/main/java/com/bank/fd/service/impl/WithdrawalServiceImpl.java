package com.bank.fd.service.impl;

import com.bank.fd.dto.request.WithdrawalRequest;
import com.bank.fd.domain.FdLifecycleStatus;
import com.bank.fd.dto.response.WithdrawalResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdInterestTransaction;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.InterestLifecycleService;
import com.bank.fd.service.WithdrawalService;
import com.bank.fd.service.AuditTrailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WithdrawalServiceImpl implements WithdrawalService {
    private final FdAccountRepository accountRepository;
    private final InterestLifecycleService lifecycleService;
    private final FdTransactionService transactionService;
    private final EventPublisher eventPublisher;
    private final FdStatementRepository statementRepository;
    private final FdInterestTransactionRepository interestRepository;
    private final AuditTrailService auditTrail;

    public WithdrawalServiceImpl(FdAccountRepository accountRepository,
                                 InterestLifecycleService lifecycleService,
                                 FdTransactionService transactionService,
                                 EventPublisher eventPublisher,
                                 FdStatementRepository statementRepository,
                                 FdInterestTransactionRepository interestRepository,
                                 AuditTrailService auditTrail) {
        this.accountRepository = accountRepository;
        this.lifecycleService = lifecycleService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.statementRepository = statementRepository;
        this.interestRepository = interestRepository;
        this.auditTrail = auditTrail;
    }

    @Override
    public WithdrawalResponse processWithdrawal(WithdrawalRequest request, String requestedBy) {
        FdAccount initial = accountRepository.findById(request.getFdAccountNo())
                .orElseThrow(() -> new FdNotFoundException(request.getFdAccountNo()));
        if (!FdLifecycleStatus.ACTIVE.name().equalsIgnoreCase(initial.getStatus())) {
            throw new InvalidOperationException("Account is not ACTIVE: " + request.getFdAccountNo());
        }

        LocalDate withdrawalDate = request.getWithdrawalDate() != null ? request.getWithdrawalDate() : LocalDate.now();
        if (withdrawalDate.isBefore(initial.getStartDate())) {
            throw new InvalidOperationException("Withdrawal date cannot be before FD start date: " + initial.getStartDate());
        }
        if (initial.getMaturityDate() != null && !withdrawalDate.isBefore(initial.getMaturityDate())) {
            throw new InvalidOperationException("Premature withdrawal date must be before maturity date: "
                    + initial.getMaturityDate());
        }

        if (Boolean.FALSE.equals(initial.getPrematureClosureAllowed())) {
            throw new InvalidOperationException("Premature closure is not allowed for FD " + initial.getFdAccountNo());
        }

        lifecycleService.processAccountThroughDate(initial.getFdAccountNo(), withdrawalDate);
        FdAccount account = accountRepository.findByIdForUpdate(initial.getFdAccountNo())
                .orElseThrow(() -> new FdNotFoundException(initial.getFdAccountNo()));
        BigDecimal balanceBeforeClosure = account.getCurrentBalance();
        BigDecimal pendingAccrued = account.getAccruedInterest();
        BigDecimal grossValue = balanceBeforeClosure.add(pendingAccrued);
        BigDecimal grossInterest = grossValue.subtract(account.getPrincipalAmount()).max(BigDecimal.ZERO);

        BigDecimal penaltyPct = account.getPrematureClosurePenaltyPct() != null
                ? account.getPrematureClosurePenaltyPct() : BigDecimal.ZERO;
        BigDecimal penaltyAmount = grossInterest.multiply(penaltyPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netInterest = grossInterest.subtract(penaltyAmount);
        BigDecimal netPayout = account.getPrincipalAmount().add(netInterest);

        List<FdInterestTransaction> pendingRecords = interestRepository
                .findByFdAccountNoAndSettlementTypeAndAccrualDateLessThanEqual(
                        account.getFdAccountNo(), "PENDING", withdrawalDate);
        pendingRecords.forEach(record -> record.setSettlementType("PREMATURE_CLOSURE"));
        interestRepository.saveAll(pendingRecords);

        account.setStatus(FdLifecycleStatus.PREMATURE_CLOSED.name());
        account.setClosureDate(withdrawalDate);
        account.setClosureType("PREMATURE");
        account.setClosureReason(request.getRemarks() == null || request.getRemarks().isBlank()
                ? "Premature closure requested" : request.getRemarks());
        account.setClosureGrossInterest(grossInterest);
        account.setClosurePenaltyAmount(penaltyAmount);
        account.setClosureNetPayout(netPayout);
        account.setClosureTransferAccountMasked(maskAccount(request.getTransferAccount()));
        account.setClosedBy(requestedBy);
        account.setCurrentBalance(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        account.setAccruedInterest(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        accountRepository.save(account);

        transactionService.recordWithdrawal(account.getFdAccountNo(), netPayout, penaltyAmount, withdrawalDate);
        if (penaltyAmount.signum() > 0) {
            transactionService.recordPenaltyDeduction(account.getFdAccountNo(), penaltyAmount, withdrawalDate);
        }

        FdStatement statement = statementRepository
                .findByFdAccountNoAndStatementDate(account.getFdAccountNo(), withdrawalDate)
                .orElseGet(FdStatement::new);
        statement.setFdAccountNo(account.getFdAccountNo());
        statement.setStatementDate(withdrawalDate);
        statement.setOpeningBalance(balanceBeforeClosure);
        statement.setInterestAccrued(pendingAccrued);
        statement.setInterestCapitalized(BigDecimal.ZERO);
        statement.setInterestPaid(netInterest);
        statement.setClosingBalance(BigDecimal.ZERO);
        statement.setAccruedInterest(BigDecimal.ZERO);
        statementRepository.save(statement);

        eventPublisher.publishFdWithdrawn(
                account.getFdAccountNo(), account.getCustomerId(), netPayout, penaltyAmount, withdrawalDate);
        auditTrail.record(requestedBy, "CUSTOMER_OR_OFFICER", "FD_PREMATURELY_CLOSED", "FD_ACCOUNT",
                account.getFdAccountNo(), "SUCCESS", Map.of(
                        "closureDate", withdrawalDate,
                        "grossInterest", grossInterest,
                        "penalty", penaltyAmount,
                        "netPayout", netPayout));

        WithdrawalResponse response = new WithdrawalResponse();
        response.setStatus(FdLifecycleStatus.PREMATURE_CLOSED.name());
        response.setMessage("FD Account prematurely closed under product penalty rules");
        response.setFdAccountNo(account.getFdAccountNo());
        response.setWithdrawalAmount(netPayout);
        response.setPrincipalReturned(account.getPrincipalAmount());
        response.setInterestEarned(netInterest);
        response.setPenaltyApplied(penaltyAmount);
        response.setEffectiveRate(account.getInterestRate());
        return response;
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) return null;
        String trimmed = account.trim();
        if (trimmed.length() <= 4) return "****";
        return "****" + trimmed.substring(trimmed.length() - 4);
    }
}
