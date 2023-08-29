package com.icoderoad.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.demo.dto.RequestOtpDTO;
import com.icoderoad.example.demo.util.OtpUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpUtil otpUtil;

    @GetMapping("/generate")
    public String generateOtp() {
        return otpUtil.generateOtp();
    }

    @PostMapping("/validate")
    public String validateOtp(@RequestBody RequestOtpDTO reqOtp) {
        return otpUtil.validateOtp(reqOtp.getUserOtp())  ? "验证成功" : "验证失败" ;
    }
    
    @GetMapping("/testEq")
    public String testEq() {
    	String userOtp = otpUtil.generateOtp();
        return otpUtil.validateOtp(userOtp) ? "验证成功" : "验证失败";
    }
}
