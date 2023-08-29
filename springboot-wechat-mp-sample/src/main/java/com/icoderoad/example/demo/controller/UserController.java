package com.icoderoad.example.demo.controller;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.demo.entity.User;
import com.icoderoad.example.demo.service.UserService;


@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(@RequestParam String userName,
                        @RequestParam String password,
                        HttpServletRequest request) {
        HttpSession session = request.getSession(true);

        // 验证登录并处理踢出逻辑
        boolean loginSuccess = userService.login(userName, password, session);

        if (loginSuccess) {
            // 登录成功，根据业务需求跳转到相应页面
            return "登陆成功";
        } else {
            // 登录失败，返回登录页或错误提示页
            return "登陆失败";
        }
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        // 注册用户
        userService.registerUser(user);

        return "注册成功";
    }
    
    @GetMapping("validate")
    public String validate(HttpServletRequest request) {
    	  HttpSession session = request.getSession(false);
    	 if( session!=null ) {
	    	 User user = !Objects.isNull(session.getAttribute("loggedInUser")) ? (User)session.getAttribute("loggedInUser"): null;
	    	 if( user!=null ) {
	    		 return "用户:" + user.getUserName() + "已登陆";
	    	 }
    	 }
    	 return "用户退出";
    }
}