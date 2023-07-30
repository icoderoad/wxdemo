package com.icoderoad.example.demo.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.demo.conf.AuthConfigProperties;

import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;

@Controller
public class AuthController {

    @Autowired
    private AuthConfigProperties authConfigProperties;

    @GetMapping("/auth/wechat")
    public String wechatLogin() {
        AuthRequest authRequest = new AuthWeChatOpenRequest(AuthConfig.builder()
                .clientId(authConfigProperties.getAppId())
                .clientSecret(authConfigProperties.getAppSecret())
                .redirectUri(authConfigProperties.getRedirectUri())
                .build());
        String authorizeUrl = authRequest.authorize(authConfigProperties.getRedirectUri()); // 传入 state 参数
        return "redirect:" + authorizeUrl;
    }

    @GetMapping("/auth/wechat/callback")
    public String wechatCallback(Model model, HttpServletRequest request, AuthCallback callback) {
    	AuthUser authUser = null;
        AuthRequest authRequest = getAuthRequest();
        AuthResponse response = authRequest.login(callback);
        if(response.getData() instanceof AuthUser) {
            authUser =  (AuthUser)response.getData();
        }
      
        model.addAttribute("userDetails", authUser);
        return "user-details";
        
    }
    
    private AuthRequest getAuthRequest() {
       
        AuthConfig.AuthConfigBuilder config = AuthConfig.builder().clientId(authConfigProperties.getAppId());
        config.clientSecret(authConfigProperties.getAppSecret());
        config.redirectUri(authConfigProperties.getRedirectUri());
        config.ignoreCheckState(true);
        AuthRequest authRequest = new AuthWeChatOpenRequest(config.build());
         
        return authRequest;
    }

}


