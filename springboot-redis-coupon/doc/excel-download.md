使用springboot + mybatis-plus + Apache POI  实现优惠券 Excel 文件下载导出功能

在完成“使用springboot + mybatis-plus + OpenPDF 实现优惠券 pdf 文件导出”课程基础上，将 Apache POI 依赖添加到 pom.xml：

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version> 
</dependency>
```

在 CouponService 中实现生成 Excel 文件的方法：

```java
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Service
public class CouponService {
    // ... (其他方法和注入)

    //导出excel优惠券
    public void exportCouponsToExcel(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Coupons");
    
        List<Coupon> coupons = couponMapper.selectList(new QueryWrapper<>());
        
        // 添加标题行
        String[] columns = {"ID", "优惠券代码", "优惠券值", "过期时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
        	 org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }
    
        int rowNum = 1; // 从第二行开始，略过标题行
    
        for (Coupon coupon : coupons) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
    
            Cell cellId = row.createCell(colNum++);
            cellId.setCellValue(coupon.getId());
    
            Cell cellCode = row.createCell(colNum++);
            cellCode.setCellValue(coupon.getCode());
    
            Cell cellValue = row.createCell(colNum++);
            cellValue.setCellValue(coupon.getValue().doubleValue());
    
            Cell cellExpiryDate = row.createCell(colNum++);
            cellExpiryDate.setCellValue(coupon.getExpiryDate().toString());
        }
    
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=coupons.xlsx");
        response.setStatus(HttpServletResponse.SC_OK);
    
        workbook.write(response.getOutputStream());
        workbook.close();
    
        System.out.println("优惠券导出 Excel 文件成功!");
    }

}
```

更新 ExportController 类，增加方法来导出 Excel 文件：

```java
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ExportController {
    // ... (其他注入和方法)

    @GetMapping("/export-coupons/excel")
    public void exportCouponsExcel(HttpServletResponse response) throws IOException {
        String fileName = "coupons.xlsx"; 
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    
        couponService.exportCouponsToExcel(response);
    }

}
```

创建测试下载视图：

在src/main/resources/templates/coupon目录下，修改 export-test.html 的HTML页面内容为：

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
		    <div class="col">
		      	<a th:href="@{/export-coupons/excel}" class="btn btn-success" >导出Excel优惠券</a>
		    </div>
		  </div>
	</div>
</body>
</html>
```


启动 Spring Boot 应用后，访问 http://localhost:8080/export-test，点击“导出Excel优惠券”连接，下载生成优惠券Excel文件。
