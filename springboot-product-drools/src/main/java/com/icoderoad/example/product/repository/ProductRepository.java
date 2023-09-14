package com.icoderoad.example.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}