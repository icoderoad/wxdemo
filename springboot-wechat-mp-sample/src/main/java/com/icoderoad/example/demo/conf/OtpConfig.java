package com.icoderoad.example.demo.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfig {
    @Value("${otp.secretKey}")
    private String otpSecretKey;

    public String getOtpSecretKey() {
        return otpSecretKey;
    }
}
