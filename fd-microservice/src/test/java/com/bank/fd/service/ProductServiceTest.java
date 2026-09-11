package com.bank.fd.service;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.exception.ProductNotFoundException;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setProductCode("FD_STD");
        sampleProduct.setProductName("Standard Fixed Deposit");
        sampleProduct.setProductType("FD");
        sampleProduct.setStatus("ACTIVE");
        sampleProduct.setMinTermMonths(3);
        sampleProduct.setMaxTermMonths(36);
        sampleProduct.setMinRate(new BigDecimal("5.00"));
        sampleProduct.setMaxRate(new BigDecimal("7.50"));
        sampleProduct.setMinDeposit(new BigDecimal("10000.00"));
        sampleProduct.setRateCapAddon(new BigDecimal("1.00"));
        sampleProduct.setPreMaturityPenaltyPct(new BigDecimal("1.00"));
    }

    @Test
    void testCreateProductSuccess() {
        ProductRequest req = new ProductRequest();
        req.setProductCode("FD_NEW");
        req.setProductName("New FD");
        req.setEffectiveDate(LocalDate.now());
        req.setMinTermMonths(6);
        req.setMaxTermMonths(60);
        req.setMinRate(new BigDecimal("6.00"));
        req.setMaxRate(new BigDecimal("8.00"));
        req.setMinDeposit(new BigDecimal("5000.00"));

        when(productRepository.findByProductCode("FD_NEW")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ApiResponse res = productService.createProduct(req, "ADMIN");

        assertTrue(res.isSuccess());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testCreateProductDuplicateThrowsException() {
        ProductRequest req = new ProductRequest();
        req.setProductCode("FD_STD");

        when(productRepository.findByProductCode("FD_STD")).thenReturn(Optional.of(sampleProduct));

        assertThrows(InvalidOperationException.class, () -> productService.createProduct(req, "ADMIN"));
    }

    @Test
    void testValidateProductForFdSuccess() {
        when(productRepository.findByProductCode("FD_STD")).thenReturn(Optional.of(sampleProduct));

        Product validated = productService.validateProductForFd("FD_STD", 12, new BigDecimal("20000.00"));

        assertNotNull(validated);
        assertEquals("FD_STD", validated.getProductCode());
    }

    @Test
    void testValidateProductTenureTooShort() {
        when(productRepository.findByProductCode("FD_STD")).thenReturn(Optional.of(sampleProduct));

        assertThrows(InvalidOperationException.class, () ->
                productService.validateProductForFd("FD_STD", 1, new BigDecimal("20000.00")));
    }

    @Test
    void testValidateProductDepositTooLow() {
        when(productRepository.findByProductCode("FD_STD")).thenReturn(Optional.of(sampleProduct));

        assertThrows(InvalidOperationException.class, () ->
                productService.validateProductForFd("FD_STD", 12, new BigDecimal("5000.00")));
    }

    @Test
    void testValidateProductInactive() {
        sampleProduct.setStatus("INACTIVE");
        when(productRepository.findByProductCode("FD_STD")).thenReturn(Optional.of(sampleProduct));

        assertThrows(InvalidOperationException.class, () ->
                productService.validateProductForFd("FD_STD", 12, new BigDecimal("20000.00")));
    }
}
