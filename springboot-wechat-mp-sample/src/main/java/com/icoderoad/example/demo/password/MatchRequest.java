package com.icoderoad.example.demo.password;

import lombok.Data;

@Data
public class MatchRequest {
    private String rawPassword;
    private String encodedPassword;
}