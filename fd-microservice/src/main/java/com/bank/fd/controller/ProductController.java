package com.bank.fd.controller;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.service.ProductService;
import com.bank.fd.service.AuditTrailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.integrations.product.mode", havingValue = "local-demo", matchIfMissing = true)
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final AuditTrailService auditTrailService;

    public ProductController(ProductService productService, AuditTrailService auditTrailService) {
        this.productService = productService;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createProduct(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        String createdBy = authentication != null ? authentication.getName() : "ADMIN";
        ApiResponse response = productService.createProduct(request, createdBy);
        auditTrailService.record(createdBy, "ADMIN", "PRODUCT_CREATED", "FD_PRODUCT",
                request.getProductCode(), "SUCCESS", Map.of("productName", request.getProductName()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable String code, @Valid @RequestBody ProductRequest request,
                                                     Authentication authentication) {
        ApiResponse response = productService.updateProduct(code, request);
        auditTrailService.record(authentication.getName(), "ADMIN", "PRODUCT_UPDATED", "FD_PRODUCT",
                code, "SUCCESS", Map.of("productName", request.getProductName()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(required = false, defaultValue = "FD") String type,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status) {
        return ResponseEntity.ok(productService.searchProducts(type, status));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Product> getProduct(@PathVariable String code) {
        return ResponseEntity.ok(productService.getProduct(code));
    }
}
