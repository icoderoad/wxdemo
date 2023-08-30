package com.icoderoad.example.orders.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.icoderoad.example.orders.entity.Order;

public interface OrderService {
    Page<Order> getOrdersByStatus(boolean orderStatus, Pageable pageable);
}