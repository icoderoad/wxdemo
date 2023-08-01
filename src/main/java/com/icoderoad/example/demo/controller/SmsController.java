package com.icoderoad.example.demo.controller;

import com.icoderoad.example.demo.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsController {

    @Autowired
    private SmsService smsService;

    // 发送短信验证码
    @PostMapping("/send-otp")
    public String sendOtpCode(@RequestParam String phoneNumber) {
        if (smsService.sendOtpCode(phoneNumber)) {
            return "短信验证码发送成功";
        } else {
            return "短信验证码发送失败";
        }
    }

    // 验证短信验证码
    @PostMapping("/verify-otp")
    public String verifyOtpCode(@RequestParam String phoneNumber, @RequestParam String otpCode) {
        if (smsService.verifyOtpCode(phoneNumber, otpCode)) {
            return "短信验证码验证通过";
        } else {
            return "短信验证码验证失败";
        }
    }
}
