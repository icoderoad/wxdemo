使用springboot + mybatis-plus + OpenPDF 实现优惠券 pdf 文件导出功能

在完成“使用springboot + mybatis-plus + OpenPDF 实现优惠券 騕 文件导出功能”课程基础上，将OpenPDF库添加到pom.xml文件中：

```xml
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.30</version> <!-- Use the latest version -->
</dependency
```

这个库允许创建PDF文档并支持中文内容。

在CouponService中实现生成PDF文件的方法：

```java
package com.icoderoad.example.demo.service;

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

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Service
public class CouponService {
    // ... (其他方法和注入)


    public void exportCouponsToPDF(HttpServletResponse response) throws IOException, DocumentException {
        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            
            //支持导出文件中包含中文
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED); // 使用中文字体
            Font fontChinese = new Font(bfChinese, 12, Font.NORMAL);
            
            List<Coupon> coupons = couponMapper.selectList(new QueryWrapper<>());
            for (Coupon coupon : coupons) {
                String couponInfo = "ID: " + coupon.getId() + "\n"
                        + "优惠券代码: " + coupon.getCode() + "\n"
                        + "优惠券值: " + coupon.getValue() + "\n"
                        + "过期时间: " + coupon.getExpiryDate() + "\n\n";
                Paragraph paragraph = new Paragraph(couponInfo, fontChinese); // 使用中文字体
                document.add(paragraph);
            }
        }
    
        System.out.println("优惠券导出 PDF 文件成功!");
    }

}
```

在 ExportController 增加 exportCouponsPdf 方法：

```java
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ExportController {
    // ... (其他注入和方法)

    @GetMapping("/export-coupons/pdf")
    public void exportCouponsPdf(HttpServletResponse response) throws IOException, DocumentException {
        String fileName = "coupons.pdf"; 
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/pdf; charset=UTF-8");
    
        couponService.exportCouponsToPDF(response);
    }

}
```

在这个示例中，我们创建了一个名为/export-coupons的GET请求的Controller方法，用于触发导出优惠券数据到pdf文件的下载功能。

创建用于测试导出连接的视图页面：

修改 export-test.html 的HTML页面，在src/main/resources/templates/coupon目录下。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>导出 CSV 文件测试</title>
     <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <h2>导出 CSV 文件测试</h2>
   
    <div class="container text-center">
		  <div class="row">
		    <div class="col">
		       <a th:href="@{/export-coupons/csv}" class="btn btn-secondary">导出CSV优惠券</a>
		    </div>
		    <div class="col">
		      	<a th:href="@{/export-coupons/pdf}" class="btn btn-danger" >导出PDF优惠券</a>
		    </div>
		  </div>
	</div>
</body>
</html>
```


这个页面包含一个导出PDF优惠券链接，点击链接将触发导出优惠券数据到PDF文件的下载功能。