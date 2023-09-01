package com.icoderoad.example.product.service.impl;


import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.product.entity.Product;
import com.icoderoad.example.product.mapper.ProductMapper;
import com.icoderoad.example.product.service.ProductService;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}