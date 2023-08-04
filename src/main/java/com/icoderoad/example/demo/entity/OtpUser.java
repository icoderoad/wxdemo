package com.icoderoad.example.demo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("otp_user")
public class OtpUser {
    private Long id;
    private String userName;
    private String password;
    private String nickName;
    private Date createTime;
    private String secretKey;
}