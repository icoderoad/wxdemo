package com.icoderoad.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final Environment environment;

    @Autowired
    public EmailService(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
        String fromEmail = environment.getProperty("app.email.from");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("重置密码链接");
        message.setText("请点击下面的链接重置您的密码：" + resetLink);

        mailSender.send(message);
    }
}