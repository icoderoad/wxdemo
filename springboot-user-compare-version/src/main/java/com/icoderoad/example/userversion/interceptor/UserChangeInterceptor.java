package com.icoderoad.example.userversion.interceptor;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.icoderoad.example.userversion.entity.User;
import com.icoderoad.example.userversion.entity.UserVersion;
import com.icoderoad.example.userversion.repository.UserRepository;

@Component
public class UserChangeInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository; // UserRepository是用于MongoDB操作的接口

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在请求处理前记录用户信息的变化，存储到MongoDB中的历史版本

        // 1. 获取当前用户信息
        String userName = getCurrentUserName(); // 实现获取当前用户ID的逻辑
        User currentUser = userRepository.findByUsername(userName);
        if( currentUser == null ) {
        	  currentUser = new User();
        	  currentUser.setUsername("admin");
        	  currentUser.setEmail("admin@icoderoad.com");
        }
        createUserVersion(currentUser);

        return true;
    }

    private String getCurrentUserName() {
        // 实现获取当前用户ID的逻辑，可以使用Spring Security或其他方式
        // 返回当前用户的唯一标识符
        return "admin"; // 临时示例用户ID
    }
    
    private boolean isUserInfoChanged(User user, UserVersion version) {
        // 在这里添加逻辑来比较用户信息是否发生更改
        return !user.getUsername().equals(version.getUsername())
            || !user.getEmail().equals(version.getEmail())
           ;
    }
    
    private void createUserVersion(User user) {
        // 获取用户的版本历史
        List<UserVersion> versions = user.getVersionHistory();

        if (versions.isEmpty() || isUserInfoChanged(user, versions.get(versions.size() - 1))) {
            // 仅在用户信息发生更改或无版本历史记录时创建新的用户版本
            UserVersion newUserVersion = new UserVersion();
            newUserVersion.setUsername(user.getUsername());
            newUserVersion.setEmail(user.getEmail());
            // 设置其他版本信息字段

            // 将新版本添加到用户的版本历史
            versions.add(newUserVersion);

            // 保存用户对象以更新版本历史
            userRepository.save(user);
        }
    }
}