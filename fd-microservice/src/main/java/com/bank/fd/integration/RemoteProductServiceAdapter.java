package com.bank.fd.integration;

import com.bank.fd.dto.request.ProductRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.exception.ProductNotFoundException;
import com.bank.fd.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.integrations.product.mode", havingValue = "remote")
public class RemoteProductServiceAdapter implements ProductService {
    private final RestClient client;

    public RemoteProductServiceAdapter(RestClient.Builder builder,
                                       @Value("${app.integrations.product.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public ApiResponse createProduct(ProductRequest request, String createdBy) {
        return client.post().uri("/api/product").header("X-Actor-Id", createdBy)
                .body(request).retrieve().body(ApiResponse.class);
    }

    @Override
    public ApiResponse updateProduct(String code, ProductRequest request) {
        return client.put().uri("/api/product/{code}", code).body(request)
                .retrieve().body(ApiResponse.class);
    }

    @Override
    public Product getProduct(String code) {
        try {
            Product product = client.get().uri("/api/product/{code}", code)
                    .retrieve().body(Product.class);
            if (product == null) throw new ProductNotFoundException("Product not found with code: " + code);
            return product;
        } catch (ProductNotFoundException error) {
            throw error;
        } catch (Exception error) {
            throw new ProductNotFoundException("Product Service lookup failed for code: " + code);
        }
    }

    @Override
    public List<Product> searchProducts(String type, String status) {
        return client.get().uri(uri -> uri.path("/api/product/search")
                        .queryParam("type", type == null ? "FD" : type)
                        .queryParam("status", status == null ? "ACTIVE" : status).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Product> getAllProducts() {
        return client.get().uri("/api/product/all").retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Product validateProductForFd(String productCode, Integer termMonths, BigDecimal principal) {
        Product product = getProduct(productCode);
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new InvalidOperationException("Product is not ACTIVE: " + productCode);
        }
        if (termMonths == null || termMonths < product.getMinTermMonths() || termMonths > product.getMaxTermMonths()) {
            throw new InvalidOperationException("Requested tenure is outside product limits");
        }
        if (principal == null || principal.compareTo(product.getMinDeposit()) < 0
                || (product.getMaxDeposit() != null && principal.compareTo(product.getMaxDeposit()) > 0)) {
            throw new InvalidOperationException("Requested amount is outside product limits");
        }
        return product;
    }
}
