package com.icoderoad.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String indexPage() {
        return "jwt/index"; // 返回 jwt 目录下的 index.html 页面
    }

    @GetMapping("/jwt/login")
    public String loginPage() {
        return "jwt/login"; // 返回 jwt 目录下的 login.html 页面
    }

    @GetMapping("/jwt/user-details")
    public String userDetailsPage() {
        return "jwt/userDetails"; // 返回 jwt 目录下的 userDetails.html 页面
    }
}
