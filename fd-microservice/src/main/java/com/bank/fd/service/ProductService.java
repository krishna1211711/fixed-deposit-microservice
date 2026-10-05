package com.bank.fd.service;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.helper.CurrencyRules;
import com.bank.fd.helper.FdBusinessRules;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ProductService {
    ApiResponse createProduct(ProductRequest request, String createdBy);
    ApiResponse updateProduct(String code, ProductRequest request);
    Product getProduct(String code);
    List<Product> searchProducts(String type, String status);
    List<Product> getAllProducts();
    Product validateProductForFd(String productCode, Integer termMonths, BigDecimal principal);

    /** One authoritative contract validator shared by calculator/opening workflow/account booking. */
    default Product validateOpeningTerms(FdAccountCreateRequest request) {
        Product product = validateProductForFd(
                request.getProductCode(), request.getTermMonths(), request.getPrincipalAmount());
        String currency = CurrencyRules.normalizeCode(
                request.getCurrency() == null ? product.getCurrency() : request.getCurrency());
        if (!currency.equalsIgnoreCase(product.getCurrency())) {
            throw new InvalidOperationException(
                    "Product " + product.getProductCode() + " is denominated in " + product.getCurrency());
        }
        Set<String> compounding = product.getAllowedCompoundingFrequencies();
        if (compounding == null || compounding.isEmpty()) {
            compounding = Set.of(FdBusinessRules.normalizeFrequency(product.getCompoundingFrequency()));
        }
        Set<String> payout = product.getAllowedPayoutFrequencies();
        if (payout == null || payout.isEmpty()) payout = Set.of("MATURITY");
        FdBusinessRules.requireCompounding(
                request.getCompoundingFrequency() == null
                        ? product.getCompoundingFrequency() : request.getCompoundingFrequency(), compounding);
        FdBusinessRules.requirePayout(
                request.getPayoutFrequency() == null ? "MATURITY" : request.getPayoutFrequency(), payout);
        FdBusinessRules.requireMaturityInstruction(request.getMaturityInstruction());
        return product;
    }
}
