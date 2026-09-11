package com.bank.fd.mapper;

import com.bank.fd.entity.Product;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProductMapper {

    public Product toEntity(Map<String, Object> dto) {
        Product product = new Product();
        // Additional mapping logic here
        return product;
    }

    public Map<String, Object> toResponse(Product product) {
        Map<String, Object> response = new HashMap<>();
        if (product != null) {
            response.put("entityType", "Product");
            // Map other fields
        }
        return response;
    }
}
