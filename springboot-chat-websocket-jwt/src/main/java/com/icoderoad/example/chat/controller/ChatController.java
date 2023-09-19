package com.icoderoad.example.chat.controller;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.chat.entity.ChatMessage;
import com.icoderoad.example.chat.entity.User;
import com.icoderoad.example.chat.repository.UserRepository;
import com.icoderoad.example.chat.util.JwtTokenUtil;

@Controller
public class ChatController {
  
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
    private SimpMessagingTemplate messagingTemplate;

  	@GetMapping("/login")
    public String loginPage() {
        return "login";
    }
  
    @GetMapping("/chat")
    public String chatPage(HttpServletRequest request, Model model) {
        // 从请求中获取 JWT Token
        String jwtToken = extractJwtTokenFromRequest(request);
			
        // 检查 JWT Token 是否有效
        if (jwtTokenUtil.validateToken(jwtToken)) {
            // 用户已登录，显示消息发送页面
        	String currentUsername = jwtTokenUtil.getUsernameFromToken(jwtToken);
        	model.addAttribute("currentUsername", currentUsername);
        	 List<User> users = userRepository.findAllExceptCurrentUser(currentUsername);

             model.addAttribute("users", users);
             model.addAttribute("jwtToken", jwtToken);
            return "chat";
        } else {
            // 用户未登录，重定向到登录页面
            return "redirect:/login";
        }
    }

    @MessageMapping("/sendSystemMessage")
    public void handleMessage(@Payload ChatMessage message) {
       
            // 在这里处理消息
            String content = message.getMessage();
            String sender = message.getSender();

            // 构建响应消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage("系统回复：" + content);
            chatMessage.setSender(sender);

            // 发送系统消息给所有用户
            messagingTemplate.convertAndSend("/user/queue/system", chatMessage);
       
    }
  
    @MessageMapping("/chat/{username}")
    public void handlePrivateMessage(@DestinationVariable("username") String username,Principal principal, ChatMessage message) {
    	// 获取当前认证的用户
        String sender = principal.getName();
        String recipient =username;

        // 构建私密消息
        ChatMessage privateMessage = new ChatMessage();
        privateMessage.setSender(sender);
        privateMessage.setRecipient(recipient);
        privateMessage.setMessage(message.getMessage());

        // 发送私密消息给目标用户
        messagingTemplate.convertAndSendToUser(recipient, "/queue/private", privateMessage);
    }
    
    // 从请求中提取 JWT Token 的方法
    private String extractJwtTokenFromRequest(HttpServletRequest request) {
        // 从请求头或参数中提取 JWT Token
        Object jwtToken = request.getSession().getAttribute("jwt-token");
        if (jwtToken != null && jwtToken.toString().startsWith("Bearer ")) {
            return jwtToken.toString().substring(7); // 去掉 "Bearer " 前缀
        }
        return null;
    }
}