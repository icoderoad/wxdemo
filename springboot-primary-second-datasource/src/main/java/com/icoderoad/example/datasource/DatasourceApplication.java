package com.icoderoad.example.datasource;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.icoderoad.example.datasource.entity.primary.User;
import com.icoderoad.example.datasource.entity.secondary.SecUser;
import com.icoderoad.example.datasource.repository.primary.PrimaryUserRepository;
import com.icoderoad.example.datasource.repository.secondary.SecondaryUserRepository;

@SpringBootApplication
public class DatasourceApplication {
    @Autowired
    private PrimaryUserRepository primaryUserRepository;

    @Autowired
    private SecondaryUserRepository secondaryUserRepository;

    public static void main(String[] args) {
        SpringApplication.run(DatasourceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // 初始化主数据源用户
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUsername("主数据源用户 " + i);
            user.setEmail("primaryuser" + i + "@example.com");
            primaryUserRepository.save(user);
        }

        // 初始化第二个数据源用户
        for (int i = 1; i <= 10; i++) {
        	SecUser user = new SecUser();
            user.setUsername("从数据源用户 " + i);
            user.setEmail("secondaryuser" + i + "@example.com");
            secondaryUserRepository.save(user);
        }
    }
}