package com.bank.fd.service.impl;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.exception.ProductNotFoundException;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import com.bank.fd.helper.FdBusinessRules;

@Service
@ConditionalOnProperty(name = "app.integrations.product.mode", havingValue = "local-demo", matchIfMissing = true)
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ApiResponse createProduct(ProductRequest req, String createdBy) {
        if (productRepository.findByProductCode(req.getProductCode()).isPresent()) {
            throw new InvalidOperationException("Product code already exists: " + req.getProductCode());
        }
        validateProductRanges(req);
        Product product = new Product();
        product.setProductCode(req.getProductCode());
        product.setProductName(req.getProductName());
        product.setProductType(req.getProductType() != null ? req.getProductType() : "FD");
        product.setCurrency(req.getCurrency() != null ? req.getCurrency() : "INR");
        product.setEffectiveDate(req.getEffectiveDate());
        product.setMinTermMonths(req.getMinTermMonths());
        product.setMaxTermMonths(req.getMaxTermMonths());
        product.setMinRate(req.getMinRate());
        product.setMaxRate(req.getMaxRate());
        product.setMinDeposit(req.getMinDeposit());
        product.setMaxDeposit(req.getMaxDeposit());
        product.setRateCapAddon(req.getRateCapAddon() != null ? req.getRateCapAddon() : new BigDecimal("2.00"));
        product.setCategoryAddonsStackable(req.getCategoryAddonsStackable() == null || req.getCategoryAddonsStackable());
        product.setPreMaturityPenaltyPct(req.getPreMaturityPenaltyPct() != null ? req.getPreMaturityPenaltyPct() : new BigDecimal("1.00"));
        Set<String> compoundingOptions = FdBusinessRules.normalizedSet(
                req.getAllowedCompoundingFrequencies(), FdBusinessRules.COMPOUNDING_FREQUENCIES);
        Set<String> payoutOptions = FdBusinessRules.normalizedSet(
                req.getAllowedPayoutFrequencies(), FdBusinessRules.PAYOUT_FREQUENCIES);
        String defaultCompounding = req.getCompoundingFrequency() != null
                ? FdBusinessRules.normalizeFrequency(req.getCompoundingFrequency()) : "QUARTERLY";
        if (!compoundingOptions.contains(defaultCompounding)) {
            throw new InvalidOperationException("Default compounding frequency must be one of the allowed options");
        }
        product.setCompoundingFrequency(defaultCompounding);
        product.setAllowedCompoundingFrequencies(compoundingOptions);
        product.setAllowedPayoutFrequencies(payoutOptions);
        product.setDayCountConvention(req.getDayCountConvention() != null ? req.getDayCountConvention() : "ACTUAL_365");
        if (!"ACTUAL_365".equals(product.getDayCountConvention())) {
            throw new InvalidOperationException("Only ACTUAL_365 is supported by this project");
        }
        product.setPrematureClosureAllowed(req.getPrematureClosureAllowed() == null || req.getPrematureClosureAllowed());
        product.setStatus("ACTIVE");
        product.setCreatedBy(createdBy);
        product.setCreatedAt(LocalDateTime.now());

        productRepository.save(product);
        return ApiResponse.success("Product created successfully", product);
    }

    @Override
    public ApiResponse updateProduct(String code, ProductRequest req) {
        Product product = getProduct(code);
        if (req.getProductName() != null) product.setProductName(req.getProductName());
        if (req.getMinRate() != null) product.setMinRate(req.getMinRate());
        if (req.getMaxRate() != null) product.setMaxRate(req.getMaxRate());
        if (req.getMinTermMonths() != null) product.setMinTermMonths(req.getMinTermMonths());
        if (req.getMaxTermMonths() != null) product.setMaxTermMonths(req.getMaxTermMonths());
        if (req.getMinDeposit() != null) product.setMinDeposit(req.getMinDeposit());
        if (req.getMaxDeposit() != null) product.setMaxDeposit(req.getMaxDeposit());
        if (req.getRateCapAddon() != null) product.setRateCapAddon(req.getRateCapAddon());
        if (req.getCategoryAddonsStackable() != null) product.setCategoryAddonsStackable(req.getCategoryAddonsStackable());
        if (req.getPreMaturityPenaltyPct() != null) product.setPreMaturityPenaltyPct(req.getPreMaturityPenaltyPct());
        if (req.getAllowedCompoundingFrequencies() != null) {
            product.setAllowedCompoundingFrequencies(FdBusinessRules.normalizedSet(
                    req.getAllowedCompoundingFrequencies(), FdBusinessRules.COMPOUNDING_FREQUENCIES));
        }
        if (req.getAllowedPayoutFrequencies() != null) {
            product.setAllowedPayoutFrequencies(FdBusinessRules.normalizedSet(
                    req.getAllowedPayoutFrequencies(), FdBusinessRules.PAYOUT_FREQUENCIES));
        }
        if (req.getCompoundingFrequency() != null) {
            String frequency = FdBusinessRules.normalizeFrequency(req.getCompoundingFrequency());
            if (!product.getAllowedCompoundingFrequencies().contains(frequency)) {
                throw new InvalidOperationException("Default compounding frequency must be allowed by the product");
            }
            product.setCompoundingFrequency(frequency);
        }
        if (req.getPrematureClosureAllowed() != null) product.setPrematureClosureAllowed(req.getPrematureClosureAllowed());

        if (product.getMinDeposit() != null && product.getMaxDeposit() != null
                && product.getMaxDeposit().compareTo(product.getMinDeposit()) < 0) {
            throw new InvalidOperationException("Maximum deposit must be greater than or equal to minimum deposit");
        }

        productRepository.save(product);
        return ApiResponse.success("Product updated successfully", product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(String code) {
        return productRepository.findByProductCode(code)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String type, String status) {
        String queryType = type != null ? type : "FD";
        String queryStatus = status != null ? status : "ACTIVE";
        return productRepository.findByProductTypeAndStatus(queryType, queryStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Product validateProductForFd(String productCode, Integer termMonths, BigDecimal principal) {
        Product product = getProduct(productCode);
        if (product.getAllowedCompoundingFrequencies() == null || product.getAllowedCompoundingFrequencies().isEmpty()) {
            String legacyFrequency = FdBusinessRules.normalizeFrequency(product.getCompoundingFrequency());
            product.setAllowedCompoundingFrequencies(Set.of(
                    legacyFrequency != null ? legacyFrequency : "QUARTERLY"));
        }
        if (product.getAllowedPayoutFrequencies() == null || product.getAllowedPayoutFrequencies().isEmpty()) {
            product.setAllowedPayoutFrequencies(Set.of("MATURITY"));
        }
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new InvalidOperationException("Product is not ACTIVE: " + productCode);
        }
        if (termMonths < product.getMinTermMonths() || termMonths > product.getMaxTermMonths()) {
            throw new InvalidOperationException("Term months (" + termMonths + ") must be between " +
                    product.getMinTermMonths() + " and " + product.getMaxTermMonths());
        }
        if (principal.compareTo(product.getMinDeposit()) < 0) {
            throw new InvalidOperationException("Principal amount (" + principal + ") is below minimum deposit requirement (" + product.getMinDeposit() + ")");
        }
        if (product.getMaxDeposit() != null && principal.compareTo(product.getMaxDeposit()) > 0) {
            throw new InvalidOperationException("Principal amount (" + principal + ") exceeds maximum deposit limit (" + product.getMaxDeposit() + ")");
        }
        return product;
    }

    private void validateProductRanges(ProductRequest req) {
        if (req.getMinTermMonths() != null && req.getMaxTermMonths() != null
                && req.getMaxTermMonths() < req.getMinTermMonths()) {
            throw new InvalidOperationException("Maximum tenure must be greater than or equal to minimum tenure");
        }
        if (req.getMinRate() != null && req.getMaxRate() != null
                && req.getMaxRate().compareTo(req.getMinRate()) < 0) {
            throw new InvalidOperationException("Maximum interest rate must be greater than or equal to minimum interest rate");
        }
        if (req.getMinDeposit() != null && req.getMaxDeposit() != null
                && req.getMaxDeposit().compareTo(req.getMinDeposit()) < 0) {
            throw new InvalidOperationException("Maximum deposit must be greater than or equal to minimum deposit");
        }
    }
}
