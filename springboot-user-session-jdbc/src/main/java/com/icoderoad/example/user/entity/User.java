package com.icoderoad.example.user.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class User implements Serializable {
    private String username;
    private String password;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}