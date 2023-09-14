package com.icoderoad.example.product.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.product.entity.Product;
import com.icoderoad.example.product.service.DiscountService;
import com.icoderoad.example.product.service.ProductService;

@Controller
public class ProductController {
    @Autowired
    private ProductService productService;
    
    @Autowired
    private DiscountService discountService;
    
    @GetMapping("/")
    public String index(Model model) {
        List<Product> products = productService.getAllProducts();
        for (Product product : products) {
            discountService.applyDiscount(product);
        }
        model.addAttribute("products", products);
        return "index";
    }
}