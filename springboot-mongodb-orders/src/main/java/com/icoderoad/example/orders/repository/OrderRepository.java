package com.icoderoad.example.orders.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.orders.entity.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
}