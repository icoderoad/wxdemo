package com.icoderoad.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String userName;
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDateTime createTime;
}
