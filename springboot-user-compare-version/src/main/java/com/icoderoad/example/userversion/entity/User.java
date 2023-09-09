package com.icoderoad.example.userversion.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String _id;
    private String userId;
    private String username;
    private String email;
    private List<UserVersion> versionHistory = new ArrayList<>();

    public void addToVersionHistory(UserVersion version) {
        this.versionHistory.add(version);
    }

}