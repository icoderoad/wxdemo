package com.icoderoad.example.userversion.entity;

import java.util.Date;

import lombok.Data;

@Data
public class UserVersion {
    private Date timestamp;
    private String username;
    private String email;
}