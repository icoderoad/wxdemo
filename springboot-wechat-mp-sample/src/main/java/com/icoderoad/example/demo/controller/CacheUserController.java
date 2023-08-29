package com.icoderoad.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.demo.entity.CacheUser;
import com.icoderoad.example.demo.service.CacheUserService;

@RestController
@RequestMapping("/users")
public class CacheUserController {

    @Autowired
    private CacheUserService userService;

    @PostMapping("/get")
    public ResponseEntity<CacheUser> getUser(@RequestBody CacheUser user) {
    	CacheUser createdUser = userService.getUserById(user.getId());
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("update/{userId}")
    public ResponseEntity<CacheUser> updateUser(@PathVariable Long userId, @RequestBody CacheUser user) {
    	CacheUser updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("del/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}