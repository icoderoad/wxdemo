package com.icoderoad.example.demo.entity;


import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("fpassword_user")
public class FPUser {

    private Long id;
    private String userName;
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDateTime createTime;

}