package com.bank.fd.service;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ApiResponse createProduct(ProductRequest request, String createdBy);
    ApiResponse updateProduct(String code, ProductRequest request);
    Product getProduct(String code);
    List<Product> searchProducts(String type, String status);
    List<Product> getAllProducts();
    Product validateProductForFd(String productCode, Integer termMonths, BigDecimal principal);
}
