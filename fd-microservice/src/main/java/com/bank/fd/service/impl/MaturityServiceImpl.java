package com.bank.fd.service.impl;

import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.InterestEngineService;
import com.bank.fd.service.MaturityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MaturityServiceImpl implements MaturityService {

    private final FdAccountRepository accountRepository;
    private final InterestEngineService interestEngineService;
    private final FdTransactionService transactionService;
    private final EventPublisher eventPublisher;
    private final FdStatementRepository statementRepository;

    public MaturityServiceImpl(FdAccountRepository accountRepository,
                               InterestEngineService interestEngineService,
                               FdTransactionService transactionService,
                               EventPublisher eventPublisher,
                               FdStatementRepository statementRepository) {
        this.accountRepository = accountRepository;
        this.interestEngineService = interestEngineService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.statementRepository = statementRepository;
    }

    @Override
    public int processMaturedAccounts(LocalDate today) {
        List<FdAccount> maturedAccounts = accountRepository.findMaturedAccounts(today);
        int count = 0;

        for (FdAccount account : maturedAccounts) {
            BigDecimal maturityAmount = interestEngineService.calculateMaturityAmount(account);
            account.setStatus("CLOSED");
            accountRepository.save(account);

            transactionService.recordMaturityPayout(account.getFdAccountNo(), maturityAmount);
            saveFinalStatement(account, today, maturityAmount);
            eventPublisher.publishFdMatured(account.getFdAccountNo(), account.getCustomerId(), maturityAmount, today);
            count++;
        }

        return count;
    }

    @Override
    public ApiResponse closeMaturedAccount(String fdAccountNo) {
        FdAccount account = accountRepository.findById(fdAccountNo)
                .orElseThrow(() -> new FdNotFoundException(fdAccountNo));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new InvalidOperationException("Account is not ACTIVE: " + fdAccountNo);
        }

        // Guard: only allow maturity close if the account has actually reached maturity.
        // Closing before maturity bypasses premature withdrawal penalties — route to withdrawal endpoint instead.
        LocalDate today = LocalDate.now();
        if (account.getMaturityDate() != null && account.getMaturityDate().isAfter(today)) {
            throw new InvalidOperationException(
                    "Account has not matured yet (maturity: " + account.getMaturityDate() + "). "
                    + "Use the premature withdrawal endpoint for early closure.");
        }

        BigDecimal maturityAmount = interestEngineService.calculateMaturityAmount(account);
        account.setStatus("CLOSED");
        accountRepository.save(account);

        transactionService.recordMaturityPayout(account.getFdAccountNo(), maturityAmount);
        saveFinalStatement(account, today, maturityAmount);
        eventPublisher.publishFdMatured(account.getFdAccountNo(), account.getCustomerId(), maturityAmount, today);

        return ApiResponse.success("FD Account " + fdAccountNo + " has been manually closed and payout processed");
    }

    private void saveFinalStatement(FdAccount account, LocalDate date, BigDecimal payoutAmount) {
        FdStatement statement = statementRepository
                .findByFdAccountNoAndStatementDate(account.getFdAccountNo(), date)
                .orElseGet(FdStatement::new);
        statement.setFdAccountNo(account.getFdAccountNo());
        statement.setStatementDate(date);
        statement.setOpeningBalance(account.getPrincipalAmount());
        statement.setInterestCredited(payoutAmount.subtract(account.getPrincipalAmount()));
        statement.setClosingBalance(payoutAmount);
        statementRepository.save(statement);
    }
}
