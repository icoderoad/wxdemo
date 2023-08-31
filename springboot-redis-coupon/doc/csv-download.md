使用springboot + mybatis-plus + OpenCSV 实现优惠券 CSV 文件下载导出功能

在完成“使用springboot + mybatis-plus + redis 实现优惠券生成及定时检测功能”课程基础上，将OpenCSV库添加到pom.xml文件中：

```xml
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.5.2</version> 
</dependency>

<!-- Thymeleaf -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

```

在 CouponService 类中创建一个方法来将优惠券数据导出为CSV文件：

```java
package com.icoderoad.example.coupon.service;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.Coupon;
import com.icoderoad.example.demo.mapper.CouponMapper;
import com.opencsv.CSVWriter;

@Service
public class CouponService {
    // ... (其他方法和注入)

     //导出csv优惠券
    public void exportCouponsToCSV(HttpServletResponse response) {
        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,优惠券代码,优惠券值,过期时间");
    
            List<Coupon> coupons = couponMapper.selectList(new QueryWrapper<>());
            for (Coupon coupon : coupons) {
                writer.println(
                    coupon.getId() + ","
                    + coupon.getCode() + ","
                    + coupon.getValue() + ","
                    + coupon.getExpiryDate()
                );
            }
    
            System.out.println("优惠券导出 CSV 文件成功!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
```

创建导出 ExportController 类及相应的导出方法：

```java
package com.icoderoad.example.coupon.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.coupon.service.CouponService;


@Controller
public class ExportController {
    

    @Autowired
    private CouponService couponService;
    
    @GetMapping("/export-coupons/csv")
    public void exportCoupons(HttpServletResponse response) {
        
        String fileName = "coupons.csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("text/csv; charset=UTF-8");
        //生成导出 CSV 文件
        couponService.exportCouponsToCSV(response);
    
    }
    
    @GetMapping("/export-test")
    public String exportTestPage(Model model) {
        return "/coupon/export-test"; 
    }

}
```

在这个示例中，我们创建了一个名为/export-coupons/csv的GET请求的Controller方法，用于触发导出优惠券数据到CSV文件。

创建用于测试导出连接的视图页面：

创建一个名为 export-test.html 的HTML页面，放在src/main/resources/templates/coupon目录下：

<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>导出 CSV 文件测试</title>
</head>
<body>
    <h2>导出 CSV 文件测试</h2>
    <a th:href="@{/export-coupons/csv}" >导出CSV优惠券</a>
</body>
</html>


这个页面包含一个链接，点击链接将触发导出优惠券数据到CSV文件。点击导出链接后，从浏览器下载 CSV 文件。

