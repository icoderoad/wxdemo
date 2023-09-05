package com.icoderoad.example.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}