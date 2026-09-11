package com.bank.fd.mapper;

import com.bank.fd.entity.FdTransaction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransactionMapper {

    public Map<String, Object> toResponse(FdTransaction transaction) {
        Map<String, Object> response = new HashMap<>();
        if (transaction != null) {
            response.put("entityType", "FdTransaction");
            // Map other fields
        }
        return response;
    }
}
