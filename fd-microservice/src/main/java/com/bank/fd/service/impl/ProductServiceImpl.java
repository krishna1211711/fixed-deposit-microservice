package com.bank.fd.service.impl;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.exception.ProductNotFoundException;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
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
        product.setRateCapAddon(req.getRateCapAddon() != null ? req.getRateCapAddon() : new BigDecimal("2.00"));
        product.setPreMaturityPenaltyPct(req.getPreMaturityPenaltyPct() != null ? req.getPreMaturityPenaltyPct() : new BigDecimal("1.00"));
        product.setCompoundingFrequency(req.getCompoundingFrequency() != null ? req.getCompoundingFrequency() : "QUARTERLY");
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
        if (req.getRateCapAddon() != null) product.setRateCapAddon(req.getRateCapAddon());
        if (req.getPreMaturityPenaltyPct() != null) product.setPreMaturityPenaltyPct(req.getPreMaturityPenaltyPct());
        if (req.getCompoundingFrequency() != null) product.setCompoundingFrequency(req.getCompoundingFrequency());

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
        return product;
    }
}
