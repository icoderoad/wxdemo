package com.icoderoad.example.demo.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "wechat")
public class AuthConfigProperties {
	//微信
    private String appId;
    private String appSecret;
    
    //企业微信
    private String corpId;
    private String agentId;
    
    private String redirectUri;

}