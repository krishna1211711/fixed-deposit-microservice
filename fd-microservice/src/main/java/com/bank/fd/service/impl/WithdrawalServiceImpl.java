package com.bank.fd.service.impl;

import com.bank.fd.dto.request.WithdrawalRequest;
import com.bank.fd.dto.response.WithdrawalResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdInterestTransaction;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.entity.Product;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.InterestLifecycleService;
import com.bank.fd.service.ProductService;
import com.bank.fd.service.WithdrawalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class WithdrawalServiceImpl implements WithdrawalService {
    private final FdAccountRepository accountRepository;
    private final ProductService productService;
    private final InterestLifecycleService lifecycleService;
    private final FdTransactionService transactionService;
    private final EventPublisher eventPublisher;
    private final FdStatementRepository statementRepository;
    private final FdInterestTransactionRepository interestRepository;

    public WithdrawalServiceImpl(FdAccountRepository accountRepository,
                                 ProductService productService,
                                 InterestLifecycleService lifecycleService,
                                 FdTransactionService transactionService,
                                 EventPublisher eventPublisher,
                                 FdStatementRepository statementRepository,
                                 FdInterestTransactionRepository interestRepository) {
        this.accountRepository = accountRepository;
        this.productService = productService;
        this.lifecycleService = lifecycleService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.statementRepository = statementRepository;
        this.interestRepository = interestRepository;
    }

    @Override
    public WithdrawalResponse processWithdrawal(WithdrawalRequest request, String requestedBy) {
        FdAccount initial = accountRepository.findById(request.getFdAccountNo())
                .orElseThrow(() -> new FdNotFoundException(request.getFdAccountNo()));
        if (!"ACTIVE".equalsIgnoreCase(initial.getStatus())) {
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

        Product product = productService.getProduct(initial.getProductCode());
        if (Boolean.FALSE.equals(product.getPrematureClosureAllowed())) {
            throw new InvalidOperationException("Premature closure is not allowed for product " + product.getProductCode());
        }

        lifecycleService.processAccountThroughDate(initial.getFdAccountNo(), withdrawalDate);
        FdAccount account = accountRepository.findByIdForUpdate(initial.getFdAccountNo())
                .orElseThrow(() -> new FdNotFoundException(initial.getFdAccountNo()));
        BigDecimal balanceBeforeClosure = account.getCurrentBalance();
        BigDecimal pendingAccrued = account.getAccruedInterest();
        BigDecimal grossValue = balanceBeforeClosure.add(pendingAccrued);
        BigDecimal grossInterest = grossValue.subtract(account.getPrincipalAmount()).max(BigDecimal.ZERO);

        BigDecimal penaltyPct = product.getPreMaturityPenaltyPct() != null
                ? product.getPreMaturityPenaltyPct() : BigDecimal.ZERO;
        BigDecimal penaltyAmount = grossInterest.multiply(penaltyPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netInterest = grossInterest.subtract(penaltyAmount);
        BigDecimal netPayout = account.getPrincipalAmount().add(netInterest);

        List<FdInterestTransaction> pendingRecords = interestRepository
                .findByFdAccountNoAndSettlementTypeAndAccrualDateLessThanEqual(
                        account.getFdAccountNo(), "PENDING", withdrawalDate);
        pendingRecords.forEach(record -> record.setSettlementType("PREMATURE_CLOSURE"));
        interestRepository.saveAll(pendingRecords);

        account.setStatus("PREMATURE_CLOSED");
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

        eventPublisher.publishFdWithdrawn(account.getFdAccountNo(), account.getCustomerId(), netPayout, penaltyAmount);

        WithdrawalResponse response = new WithdrawalResponse();
        response.setStatus("PREMATURE_CLOSED");
        response.setMessage("FD Account prematurely closed under product penalty rules");
        response.setFdAccountNo(account.getFdAccountNo());
        response.setWithdrawalAmount(netPayout);
        response.setPrincipalReturned(account.getPrincipalAmount());
        response.setInterestEarned(netInterest);
        response.setPenaltyApplied(penaltyAmount);
        response.setEffectiveRate(account.getInterestRate());
        return response;
    }
}
