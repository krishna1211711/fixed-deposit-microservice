package com.bank.fd.repository;

import com.bank.fd.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByProductTypeAndStatus(String type, String status);
    Optional<Product> findByProductCode(String code);
}
