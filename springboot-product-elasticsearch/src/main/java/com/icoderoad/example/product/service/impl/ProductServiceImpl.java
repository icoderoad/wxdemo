package com.icoderoad.example.product.service.impl;


import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.product.entity.Product;
import com.icoderoad.example.product.mapper.ProductMapper;
import com.icoderoad.example.product.service.ProductService;


@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
	
	@Autowired
	private ElasticsearchRestTemplate elasticsearchRestTemplate;
	

    public void importProductsToElasticsearch() {
        // 创建商品索引
        elasticsearchRestTemplate.indexOps(Product.class).create();
        
        // 查询数据库中的商品数据
        List<Product> products = this.list();
        
        // 批量导入商品数据到Elasticsearch
        elasticsearchRestTemplate.save(products);
    }
    
    public List<Product> searchProducts(String keyword) {
    	// 构建一个 NativeSearchQuery 来执行 Elasticsearch 搜索
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("name", keyword)) // 根据商品名称搜索
                .should(QueryBuilders.matchQuery("description", keyword)) // 根据商品描述搜索
            )
            .build();

        // 使用 ElasticsearchRestTemplate 执行搜索
        SearchHits<Product> searchHits = elasticsearchRestTemplate.search(searchQuery, Product.class);

        // 从搜索结果中提取商品列表
        List<Product> products = searchHits.stream()
            .map(hit -> hit.getContent())
            .collect(Collectors.toList());

        return products;
    }
}