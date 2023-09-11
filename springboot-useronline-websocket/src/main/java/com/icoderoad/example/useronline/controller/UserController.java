package com.icoderoad.example.useronline.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.useronline.entity.User;
import com.icoderoad.example.useronline.service.UserService;

@Controller
public class UserController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @MessageMapping("/hello")
    public void greeting(String message) throws Exception {
        // 处理WebSocket消息
        // 发送消息到/topic/onlineUsers
        messagingTemplate.convertAndSend("/topic/onlineUsers", message);
    }

    @GetMapping("/login")
    public String loginForm() {
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        // 根据用户名和密码验证登录
        User user = userService.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            // 登录成功
            // 发送WebSocket消息通知其他用户
            messagingTemplate.convertAndSend("/topic/onlineUsers", username + " 已登录");
            // 登录成功
            return "redirect:/login?success=true"; // 重定向到登录页并传递成功提示参数
        } else {
            // 登录失败
            return "redirect:/login?error";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 获取在线用户列表，显示在Thymeleaf视图中
        return "user/dashboard";
    }
}