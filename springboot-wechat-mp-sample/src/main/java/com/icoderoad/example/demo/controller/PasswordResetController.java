package com.icoderoad.example.demo.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.demo.entity.FPUser;
import com.icoderoad.example.demo.entity.PasswordResetToken;
import com.icoderoad.example.demo.service.EmailService;
import com.icoderoad.example.demo.service.FPUserService;
import com.icoderoad.example.demo.service.PasswordResetTokenService;

@Controller
@RequestMapping("/forgot-password")
public class PasswordResetController {
	
	@Autowired
    private  FPUserService userService;
	
	@Autowired
    private  PasswordResetTokenService tokenService;
    
	@Autowired
    private  EmailService emailService;
	
	@Autowired
    private  PasswordEncoder passwordEncoder;
	
	@Value("${app.email.reset-link}")
	private String resetLink;


    @GetMapping
    public String showForgotPasswordForm() {
        return "reset-pwd/forgot_password_form";
    }
    
    private static final int TOKEN_LENGTH = 32;
    
    @PostMapping
    public String processForgotPassword(@RequestParam("email") String email) {
    	FPUser user = userService.findByEmail(email);
        if (user != null) {
            String token = generateToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);
            tokenService.createPasswordResetToken(user.getId(), token, expiryDate);

            String resetUrl = resetLink + token;

            emailService.sendPasswordResetEmail(email, resetUrl);
        }
        return "redirect:/forgot-password?success";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token) {
        PasswordResetToken passwordResetToken = tokenService.findByToken(token);
        if (passwordResetToken != null && !passwordResetToken.isExpired()) {
            return "reset-pwd/reset_password_form";
        } else {
            return "redirect:/forgot-password?error";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password) {
        PasswordResetToken passwordResetToken = tokenService.findByToken(token);
        if (passwordResetToken != null && !passwordResetToken.isExpired()) {
        	FPUser user = userService.getById(passwordResetToken.getUserId());
            user.setPassword(passwordEncoder.encode(password));
            userService.updateById(user);
            tokenService.deleteToken(passwordResetToken.getId());
            return "reset-pwd/reset_password_succ";
        } else {
            return "redirect:/forgot-password?error";
        }
    }
    
    public  String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}