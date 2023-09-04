使用 Spring Boot+MyBatis Plus+ Elasticsearch（ES）实现批量将商品内容自动导入，使用Thymeleaf模板进行商品检索功能

在 “使用 Spring Boot + AOP + Logback 用切面来拦截商品操作日志功能实现”课程基础上，增加搜索相关功能

需要在`pom.xml`中添加 Spring Data Elasticsearch 相关依赖。

```xml
<!-- Spring Data Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

在`application.properties`中添加Elasticsearch连接信息：

```properties
spring.data.elasticsearch.cluster-nodes=localhost:9200
spring.data.elasticsearch.cluster-name=my-elasticsearch-cluster
```

ProductService 接口增加导入全部商品方法

```java
package com.icoderoad.example.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icoderoad.example.product.entity.Product;

public interface ProductService extends IService<Product> {
	
	 public void importProductsToElasticsearch();
	 
}
```

 在ProductService服务类，增加批量导入商品数据到Elasticsearch。可以使用Spring Data Elasticsearch的`ElasticsearchRestTemplate`进行索引的创建和数据导入。

```java
package com.icoderoad.example.product.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
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
}
```

在控制器增加页面视图的展示和商品检索功能。

```java
package com.icoderoad.example.product.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.product.entity.Product;
import com.icoderoad.example.product.service.ProductService;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.list());
        return "product/list";
    }
    
    @GetMapping("/import")
    public String importProducts() {
        productService.importProductsToElasticsearch();
        return "product/import-success"; // 创建一个导入成功页面
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam String keyword, Model model) {
        // 根据关键字在Elasticsearch中搜索商品
        List<Product> products = productService.searchProducts(keyword);
        model.addAttribute("products", products);
        return "product/search-results"; // 创建一个搜索结果页面
    }
}
```

 import-success.html 视图：

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>导入成功</title>

    <!-- 添加Bootstrap CDN链接 -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>

<div class="container mt-5">
    <h1>导入成功</h1>
    <p>商品数据已成功导入到Elasticsearch。</p>
</div>

</body>
</html>
```

商品搜索视图

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>商品搜索</title>

    <!-- 添加Bootstrap CDN链接 -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>

<div class="container mt-5">
    <h1>商品搜索</h1>
    <form action="/search" method="GET">
        <div class="input-group mb-3">
            <input type="text" class="form-control" placeholder="输入商品关键字" name="keyword" required>
            <div class="input-group-append">
                <button class="btn btn-primary" type="submit">搜索</button>
            </div>
        </div>
    </form>

    <!-- 显示搜索结果 -->
    <div th:if="${not #lists.isEmpty(products)}">
        <h2>搜索结果</h2>
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>名称</th>
                    <th>价格</th>
                    <th>描述</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="product : ${products}">
                    <td th:text="${product.id}"></td>
                    <td th:text="${product.name}"></td>
                    <td th:text="${product.price}"></td>
                    <td th:text="${product.description}"></td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

</body>
</html>
```

启动 Spring Boot 应用后，访问 http://localhost:8080/import，导入所有商品到 Elasticsearch，访问 http://localhost:8080/search显示商品检索页面，输入商品关键字，点击检索，检索符合条件的商品列表。