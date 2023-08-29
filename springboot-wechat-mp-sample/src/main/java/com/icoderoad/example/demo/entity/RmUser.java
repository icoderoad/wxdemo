package com.icoderoad.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("rm_user")
public class RmUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String rememberToken;
}