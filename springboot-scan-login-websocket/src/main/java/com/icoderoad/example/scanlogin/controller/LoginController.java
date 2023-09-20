package com.icoderoad.example.scanlogin.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.icoderoad.example.scanlogin.conf.LoginWebSocket;

@Controller
public class LoginController {

	@GetMapping("/")
    public String loginPage() {
        return "login"; // 返回Thymeleaf模板页面login.html
    }
	
	@GetMapping("/generateUUID")
    @ResponseBody
    public String generateUUID() {
        // 生成一个UUID并返回给前端
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
	
	@GetMapping("/confirmLogin/{uuid}")
    public String confirmLoginPage(@PathVariable("uuid") String uuid, Model model) {
		model.addAttribute("uuid",uuid);
        return "confirm_login"; // 返回确认登录页面confirm_login.html
    }

    @GetMapping("/loginSuccess/{uuid}")
    public String loginSuccessPage(@PathVariable("uuid") String uuid) {
    	LoginWebSocket.sendMessage(uuid,"确认登陆");
        return "login_success"; // 返回登录成功页面login_success.html
    }
}