package com.icoderoad.example.product.entity;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("product")
@Document(indexName = "product")
@Setting(shards = 3, replicas = 0)
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private double price;
    private String description;
}