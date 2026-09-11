package com.bank.fd.service.impl;

import com.bank.fd.dto.request.WithdrawalRequest;
import com.bank.fd.dto.response.WithdrawalResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.entity.Product;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.InterestEngineService;
import com.bank.fd.service.ProductService;
import com.bank.fd.service.WithdrawalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
public class WithdrawalServiceImpl implements WithdrawalService {

    private final FdAccountRepository accountRepository;
    private final ProductService productService;
    private final InterestEngineService interestEngineService;
    private final FdTransactionService transactionService;
    private final EventPublisher eventPublisher;
    private final FdStatementRepository statementRepository;

    public WithdrawalServiceImpl(FdAccountRepository accountRepository,
                                 ProductService productService,
                                 InterestEngineService interestEngineService,
                                 FdTransactionService transactionService,
                                 EventPublisher eventPublisher,
                                 FdStatementRepository statementRepository) {
        this.accountRepository = accountRepository;
        this.productService = productService;
        this.interestEngineService = interestEngineService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.statementRepository = statementRepository;
    }

    @Override
    public WithdrawalResponse processWithdrawal(WithdrawalRequest request, String requestedBy) {
        FdAccount account = accountRepository.findById(request.getFdAccountNo())
                .orElseThrow(() -> new FdNotFoundException(request.getFdAccountNo()));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new InvalidOperationException("Account is not ACTIVE: " + request.getFdAccountNo());
        }

        LocalDate withdrawalDate = request.getWithdrawalDate() != null
                ? request.getWithdrawalDate()
                : LocalDate.now();

        LocalDate openingDate = account.getCreatedAt().toLocalDate();
        if (withdrawalDate.isBefore(openingDate)) {
            throw new InvalidOperationException("Withdrawal date cannot be before FD opening date: " + openingDate);
        }
        if (account.getMaturityDate() != null && !withdrawalDate.isBefore(account.getMaturityDate())) {
            throw new InvalidOperationException(
                    "Premature withdrawal date must be before maturity date: " + account.getMaturityDate());
        }

        // Calculate interest accrued from account opening up to the withdrawal date
        BigDecimal accruedInterest = interestEngineService.calculateAccruedInterestForPeriod(
                account, openingDate, withdrawalDate);

        Product product = productService.getProduct(account.getProductCode());
        BigDecimal penaltyPct = product.getPreMaturityPenaltyPct() != null
                ? product.getPreMaturityPenaltyPct()
                : new BigDecimal("1.00");

        // Penalty is a percentage of the accrued interest
        BigDecimal penaltyAmount = accruedInterest
                .multiply(penaltyPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netInterest = accruedInterest.subtract(penaltyAmount);
        BigDecimal netPayout = account.getPrincipalAmount().add(netInterest);

        // Update account status
        account.setStatus("PREMATURE_CLOSED");
        accountRepository.save(account);

        // Record withdrawal net payout transaction
        transactionService.recordWithdrawal(account.getFdAccountNo(), netPayout, penaltyAmount);

        // Record penalty as a separate GL transaction for audit trail
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            transactionService.recordPenaltyDeduction(account.getFdAccountNo(), penaltyAmount);
        }

        FdStatement finalStatement = statementRepository
                .findByFdAccountNoAndStatementDate(account.getFdAccountNo(), withdrawalDate)
                .orElseGet(FdStatement::new);
        finalStatement.setFdAccountNo(account.getFdAccountNo());
        finalStatement.setStatementDate(withdrawalDate);
        finalStatement.setOpeningBalance(account.getPrincipalAmount());
        finalStatement.setInterestCredited(netInterest);
        finalStatement.setClosingBalance(netPayout);
        statementRepository.save(finalStatement);

        eventPublisher.publishFdWithdrawn(account.getFdAccountNo(), account.getCustomerId(),
                netPayout, penaltyAmount);

        WithdrawalResponse response = new WithdrawalResponse();
        response.setStatus("PREMATURE_CLOSED");
        response.setMessage("FD Account prematurely closed with penalty applied");
        response.setFdAccountNo(account.getFdAccountNo());
        response.setWithdrawalAmount(netPayout);
        response.setPrincipalReturned(account.getPrincipalAmount());
        response.setInterestEarned(netInterest);
        response.setPenaltyApplied(penaltyAmount);
        // Return the original contracted interest rate — penalty is shown separately
        response.setEffectiveRate(account.getInterestRate());

        return response;
    }
}
