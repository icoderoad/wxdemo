package com.icoderoad.example.attachment.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.icoderoad.example.attachment.entity.Attachment;
import com.icoderoad.example.attachment.service.EmailService;

@Controller
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/")
    public String showForm() {
        return "email-form";
    }

    @PostMapping("/send-email")
    public String sendEmail(@RequestParam("to") String to,
                            @RequestParam("subject") String subject,
                            @RequestParam("username") String username,
                            @RequestParam("file") MultipartFile file,
                            Model model
    		) throws IOException, MessagingException {
        List<Attachment> attachments = new ArrayList<>();
        if (!file.isEmpty()) {
            Attachment attachment = new Attachment();
            attachment.setName(file.getOriginalFilename());
            attachment.setContentId("attachment_" + System.currentTimeMillis());
            attachment.setData(file.getBytes());
            attachment.setContentType(file.getContentType());
            attachments.add(attachment);
        }

        emailService.sendEmailWithAttachment(to, subject, username, attachments);
        model.addAttribute("successMessage", "邮件已成功发送！");
        return "email-form";
    }
}