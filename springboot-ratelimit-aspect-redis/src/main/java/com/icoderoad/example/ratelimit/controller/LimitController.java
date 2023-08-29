package com.icoderoad.example.ratelimit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.icoderoad.example.ratelimit.aop.RateLimit;

@Controller
public class LimitController {

    @RateLimit(key = "limitApi", limit = 2, period = 300)
    @GetMapping("/limitApi")
    @ResponseBody
    public String limitApi() {
        return "API 数据正常响应";
    }
    
    @GetMapping("/limitTest")
    public String limitTest() {
        return "limit/rate-limit";
    }

}