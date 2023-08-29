package com.icoderoad.example.forgotpassword.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("password_reset_token")
public class PasswordResetToken {
    

    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiryDate;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}