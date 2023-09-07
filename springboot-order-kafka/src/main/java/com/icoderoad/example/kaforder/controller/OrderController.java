package com.icoderoad.example.kaforder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.icoderoad.example.kaforder.entity.Order;
import com.icoderoad.example.kaforder.service.OrderProducerService;

@Controller
public class OrderController {

    private final OrderProducerService orderProducer;

    @Autowired
    public OrderController(OrderProducerService orderProducer) {
        this.orderProducer = orderProducer;
    }

    @GetMapping("/createOrder")
    public String createOrderForm(Model model) {
        return "orders/create-order"; // 返回名为create-order的HTML视图
    }

    @PostMapping("/createOrder")
    public String createOrder(@RequestBody Order order, RedirectAttributes redirectAttributes) {
        orderProducer.produceOrder(order);
        redirectAttributes.addFlashAttribute("successMessage", "订单已创建成功！"); // 重定向时传递成功消息
        return "redirect:/createOrder";
    }
}