package com.icoderoad.example.orders.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.icoderoad.example.orders.entity.Order;
import com.icoderoad.example.orders.repository.OrderRepository;
import com.icoderoad.example.orders.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<Order> getOrdersByStatus(boolean orderStatus, Pageable pageable) {
    	// 获取所有订单数据
        List<Order> allOrders = orderRepository.findAll();

        // 使用 stream 和 filter 进行过滤
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> order.isOrder_status() == orderStatus)
                .collect(Collectors.toList());

        // 创建分页结果
        int startIndex = pageable.getPageNumber() * pageable.getPageSize();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), filteredOrders.size());
        List<Order> pageOrders = filteredOrders.subList(startIndex, endIndex);
        pageOrders.forEach(order -> {
            Date date = order.getOrder_date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate =  sdf.format(date);
            order.setFormattedOrderDate(formattedDate);
        });;
        return new PageImpl<>(pageOrders, pageable, filteredOrders.size());
    }
}