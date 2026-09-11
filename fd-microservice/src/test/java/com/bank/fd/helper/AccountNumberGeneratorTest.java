package com.bank.fd.helper;

import com.bank.fd.entity.FdAccountSequence;
import com.bank.fd.repository.FdAccountSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

    @Mock
    private FdAccountSequenceRepository sequenceRepository;

    @InjectMocks
    private AccountNumberGenerator generator;

    @Test
    void testGenerateWithExistingSequence() {
        FdAccountSequence seq = new FdAccountSequence();
        seq.setBranchCode("001");
        seq.setCurrentSeq(42L);

        when(sequenceRepository.findByBranchCodeForUpdate("001")).thenReturn(Optional.of(seq));
        when(sequenceRepository.save(any(FdAccountSequence.class))).thenReturn(seq);

        String accountNo = generator.generate("001");

        // Base = "001000043" -> sum = 0+0+1+0+0+0+0+4+3 = 8 -> checksum = 8 -> "0010000438"
        assertEquals("0010000438", accountNo);
        assertEquals(43L, seq.getCurrentSeq());
        verify(sequenceRepository).save(seq);
    }

    @Test
    void testGenerateWithNewSequence() {
        when(sequenceRepository.findByBranchCodeForUpdate("002")).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(FdAccountSequence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String accountNo = generator.generate("002");

        // Base = "002000001" -> sum = 0+0+2+0+0+0+0+0+1 = 3 -> checksum = 3 -> "0020000013"
        assertEquals("0020000013", accountNo);
    }

    @Test
    void testGenerateWithNullBranchCode() {
        when(sequenceRepository.findByBranchCodeForUpdate("001")).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(FdAccountSequence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String accountNo = generator.generate(null);

        // Branch defaults to "001", nextValue=1 -> Base = "001000001" -> sum = 2 -> "0010000012"
        assertEquals("0010000012", accountNo);
    }
}
