package com.icoderoad.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jwt_user")
public class JwtUser {
    private Long id;
    @TableField("user_name")
    private String userName;
    @TableField("password")
    private String password;
    @TableField("nick_name")
    private String nickName;
    @TableField("create_time")
    private LocalDateTime createTime;
}