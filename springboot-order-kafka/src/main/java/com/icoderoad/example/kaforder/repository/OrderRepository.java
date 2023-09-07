package com.icoderoad.example.kaforder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.kaforder.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
   
}