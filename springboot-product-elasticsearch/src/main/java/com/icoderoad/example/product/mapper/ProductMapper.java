package com.icoderoad.example.product.mapper;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.product.entity.Product;

@Repository
public interface ProductMapper extends BaseMapper<Product> {
}