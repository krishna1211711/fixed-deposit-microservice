package com.bank.fd.helper;

import com.bank.fd.entity.FdAccountSequence;
import com.bank.fd.repository.FdAccountSequenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AccountNumberGenerator {

    private final FdAccountSequenceRepository fdAccountSequenceRepository;

    public AccountNumberGenerator(FdAccountSequenceRepository fdAccountSequenceRepository) {
        this.fdAccountSequenceRepository = fdAccountSequenceRepository;
    }

    @Transactional
    public String generate(String branchCode) {
        String effectiveBranch = (branchCode != null && !branchCode.isBlank()) ? branchCode : "001";
        Optional<FdAccountSequence> sequenceOpt = fdAccountSequenceRepository.findByBranchCodeForUpdate(effectiveBranch);
        
        long nextValue;
        if (sequenceOpt.isPresent()) {
            FdAccountSequence sequence = sequenceOpt.get();
            nextValue = sequence.getCurrentSeq() + 1;
            sequence.setCurrentSeq(nextValue);
            fdAccountSequenceRepository.save(sequence);
        } else {
            nextValue = 1;
            FdAccountSequence sequence = new FdAccountSequence();
            sequence.setBranchCode(effectiveBranch);
            sequence.setCurrentSeq(nextValue);
            fdAccountSequenceRepository.save(sequence);
        }

        String paddedSequence = String.format("%06d", nextValue);
        String baseAccount = effectiveBranch + paddedSequence;
        
        int sum = 0;
        for (char c : baseAccount.toCharArray()) {
            if (Character.isDigit(c)) {
                sum += Character.getNumericValue(c);
            } else {
                sum += c; // fallback if there are characters
            }
        }
        int checksum = sum % 10;
        
        return baseAccount + checksum;
    }
}
