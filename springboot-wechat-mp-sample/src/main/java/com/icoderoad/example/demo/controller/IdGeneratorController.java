package com.icoderoad.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.demo.util.UniqueIdGenerator;

@RestController
@RequestMapping("/api")
public class IdGeneratorController {

    @Autowired
    private UniqueIdGenerator uniqueIdGenerator;

    @GetMapping("/generate-id")
    public long generateId() {
        return uniqueIdGenerator.generateUniqueId();
    }
}