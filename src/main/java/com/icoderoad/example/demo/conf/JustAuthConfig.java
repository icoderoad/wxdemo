package com.icoderoad.example.demo.conf;

import me.zhyd.oauth.config.AuthConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JustAuthConfig {

    @Autowired
    private AuthConfigProperties authConfigProperties;

    @Bean
    public AuthConfig authConfig() {
        return AuthConfig.builder()
        		.ignoreCheckState(true)
                .clientId(authConfigProperties.getAppId())
                .clientSecret(authConfigProperties.getAppSecret())
                .redirectUri(authConfigProperties.getRedirectUri()) // 回调地址，根据实际情况修改
                .build();
    }
}