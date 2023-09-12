使用Spring Boot和MinIO实现文件上传、存储和下载功能

可以按照以下步骤操作：

**准备工作**：

- 确保已经安装了Java和Spring Boot。
- 下载并启动MinIO服务器。可以在[MinIO官方网站](https://min.io/download)下载MinIO服务器，并按照官方文档进行配置和启动。

**创建Spring Boot项目**：

使用Spring Initializer或者自己手动创建一个Spring Boot项目。

**添加依赖**：

在`pom.xml`文件中添加以下依赖，以使用Thymeleaf和MinIO客户端库：

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Thymeleaf for HTML templates -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- MinIO Client -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>8.3.0</version> <!-- 查看最新版本 -->
    </dependency>
</dependencies>
```

**配置MinIO连接**：

在`application.properties` 中配置MinIO连接信息：

```properties
minio.endpoint=http://localhost:9000
minio.access-key=your-access-key
minio.secret-key=your-secret-key
minio.bucket-name=your-bucket-name

spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

请将上述`your-access-key`和`your-secret-key`替换为您的MinIO访问密钥。

**配置 MinioClient Bean**：

```java
package com.icoderoad.example.minio.conf;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

**创建Controller**：

创建一个控制器来处理文件上传、存储和下载的请求：

```java
package com.icoderoad.example.minio.controller;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;

@Controller
public class FileController {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @GetMapping("/")
    public String index(Model model) {
        List<Item> items = listFiles();
        model.addAttribute("items", items);
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(file.getOriginalFilename())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            // 处理异常
        }
        return "redirect:/";
    }

    @GetMapping("/download/{filename}")
    public void downloadFile(@PathVariable("filename") String filename,
                              HttpServletResponse response) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .build();
            InputStream stream = minioClient.getObject(args);

            // 设置响应头，告诉浏览器下载文件
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(filename, "UTF-8") + "\"");
            response.setContentType("application/octet-stream");

            // 将文件内容复制到响应输出流
            IOUtils.copy(stream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            // 处理异常
        }
    }

    private List<Item> listFiles() {
        try {
        	Iterable<Result<Item>> results=  minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
        	// 将结果转换为List<Item>
            List<Item> itemList = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                itemList.add(item);
            }
            return itemList;
        } catch (Exception e) {
            // 处理异常
            return Collections.emptyList();
        }
    }
}
```

请确保替换`your-access-key`和`your-secret-key`为MinIO的访问密钥，并根据需要修改其他配置。

**创建Thymeleaf视图**：

创建Thymeleaf视图，用于显示文件列表和上传表单。在`src/main/resources/templates`目录下创建一个`index.html`文件：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>文件上传和下载</title>
    <!-- 引入Bootstrap的CSS文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>文件上传和下载</h1>

        <h2>上传文件</h2>
        <form action="/upload" method="post" enctype="multipart/form-data">
            <div class="input-group mb-3">
                <input type="file" class="form-control" name="file" required>
                <button type="submit" class="btn btn-primary">上传</button>
            </div>
        </form>

        <h2>文件列表</h2>
        <table class="table">
            <thead>
                <tr>
                    <th>文件名</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="item : ${items}">
                    <td th:text="${item.object()}"></td>
                    <td>
                        <a th:href="@{'/download/' + ${item.object()}}" class="btn btn-success">下载</a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- 引入Bootstrap的JavaScript文件 -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.min.js"></script>
</body>
</html>
```

**运行应用**：

运行Spring Boot应用程序：`mvn spring-boot:run`。

**访问应用**：

打开浏览器并访问`http://localhost:8080`，您应该能够看到文件上传和下载的界面。

现在，您可以使用Spring Boot和MinIO实现文件上传、存储和下载功能。上传的文件将保存在MinIO存储桶中，而文件列表将显示在Thymeleaf视图中。点击文件名链接即可下载文件。