package com.icoderoad.example.demo.service;

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.RmUser;
import com.icoderoad.example.demo.mapper.RmUserMapper;

@Service
public class RmUserService {

    @Autowired
    private RmUserMapper userMapper;

    public boolean login(String username, String password, boolean rememberMe,
                         HttpServletRequest request, HttpServletResponse response) {
        RmUser user = userMapper.selectOne(new QueryWrapper<RmUser>().eq("username", username));
        if (user != null && user.getPassword().equals(password)) {
            // 登录成功
            if (rememberMe) {
                // 生成一个随机的rememberToken
                String rememberToken = UUID.randomUUID().toString();
                user.setRememberToken(rememberToken);
                userMapper.updateById(user);

                // 将rememberToken存储到Cookie中
                Cookie rememberMeCookie = new Cookie("rememberMe", rememberToken);
                rememberMeCookie.setMaxAge(7 * 24 * 60 * 60); // 设置Cookie有效期为7天
                response.addCookie(rememberMeCookie);
            }

            // 在Session中保存登录状态
            request.getSession().setAttribute("user", user);
            return true;
        }
        return false;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 清除Session中的用户信息
        request.getSession().removeAttribute("user");

        // 清除记住我功能的Cookie
        Cookie rememberMeCookie = new Cookie("rememberMe", null);
        rememberMeCookie.setMaxAge(0);
        response.addCookie(rememberMeCookie);
    }

    public RmUser getUserFromRememberMeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("rememberMe".equals(cookie.getName())) {
                    String rememberToken = cookie.getValue();
                    if (!StringUtils.isEmpty(rememberToken)) {
                        return userMapper.selectOne(new QueryWrapper<RmUser>().eq("remember_token", rememberToken));
                    }
                }
            }
        }
        return null;
    }
}