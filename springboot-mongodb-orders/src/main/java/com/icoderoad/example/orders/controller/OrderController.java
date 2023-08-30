package com.icoderoad.example.orders.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.orders.entity.Order;
import com.icoderoad.example.orders.service.OrderService;

@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String getOrderList(
            @RequestParam(name = "status", required = false, defaultValue = "false") boolean status,
            @PageableDefault(size = 10)  Pageable pageable,
            Model model) {
        Page<Order> orders = orderService.getOrdersByStatus(status, pageable);
        model.addAttribute("orders", orders);
        return "order/order-list"; // 返回Thymeleaf模板名称
    }
}