package com.icoderoad.example.product.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icoderoad.example.product.entity.Product;

public interface ProductService extends IService<Product> {
	
	 public void importProductsToElasticsearch();
	 
	 public List<Product> searchProducts(String keyword);
	 
}