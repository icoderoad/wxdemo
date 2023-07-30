package com.icoderoad.example.demo.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "wechat")
public class AuthConfigProperties {

    private String appId;
    private String appSecret;
    private String redirectUri;
}