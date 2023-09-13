Spring Boot中实现压缩文件上传、解压和使用 Thymeleaf、Bootstrap显示上传的压缩包文件列表功能

**创建Spring Boot项目：** 创建一个新的Spring Boot项目或者使用现有的项目。

**添加依赖：** 在 `pom.xml` 文件中添加以下依赖，以支持文件上传、Thymeleaf、和Bootstrap：

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

**配置文件上传目录：** 在 `application.properties` 文件中添加配置，指定文件上传目录的路径：

```properties
# Thymeleaf模板配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false

# 文件上传目录
upload.dir=/path/to/upload/directory
```

**创建文件上传和解压的服务：** 创建一个服务类来处理文件上传和解压操作。

```java
package com.icoderoad.example.compressfile.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    @Value("${upload.dir}")
    private String uploadDir;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".zip", ".rar");

    public void uploadAndExtract(MultipartFile file) throws IOException {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 保存上传的文件到指定目录
        String uploadedFilePath = uploadDir + File.separator + file.getOriginalFilename();
        file.transferTo(new File(uploadedFilePath));

        // 解压文件并保存到指定目录
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(uploadedFilePath))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String extractedFilePath = uploadDir + File.separator + "prd" + File.separator + entry.getName();
                    File extractedFile = new File(extractedFilePath);
                    extractedFile.getParentFile().mkdirs();
                    try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }

    public List<String> listUploadedFiles() {
        File directory = new File(uploadDir);
        String[] files = directory.list();

        if (files != null) {
            // 使用Java 8 Stream过滤文件列表，只返回.zip和.rar文件
            return Arrays.stream(files)
                    .filter(fileName -> ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
    
    public List<String> getFileList(String compressedFile) {
        List<String> fileList = new ArrayList<>();

        // 压缩文件完整路径
        String compressedFilePath = uploadDir + File.separator + compressedFile;

        // 使用 ZipFile 类打开压缩文件
        try  {
        	ZipFile zipFile = new ZipFile(compressedFilePath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                fileList.add(entry.getName());
            }
            zipFile.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return fileList;
    }
}
```

** 创建Controller：** 创建一个Spring MVC控制器来处理文件上传、显示上传的文件列表和下载单个文件。

```java
package com.icoderoad.example.compressfile.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.icoderoad.example.compressfile.service.FileUploadService;

@Controller
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Value("${upload.dir}")
    private String uploadDir;

    @GetMapping("/")
    public String index(Model model) {
        List<String> compressedFiles = fileUploadService.listUploadedFiles();
        model.addAttribute("compressedFiles", compressedFiles);
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
          
            String contentType = file.getContentType();
            if (contentType != null && (contentType.equals("application/zip") || 	contentType.equals("application/x-rar-compressed"))) {
                fileUploadService.uploadAndExtract(file);
                redirectAttributes.addFlashAttribute("message", "文件上传成功并解压！");
            } else {
                redirectAttributes.addFlashAttribute("error", "只能上传.zip和.rar文件。");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "文件上传失败：" + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam("fileName") String fileName, HttpServletResponse response) {
        try {
            File file = new File(uploadDir  + File.separator + "prd" + File.separator + fileName);
            FileInputStream fis = new FileInputStream(file);

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            fis.close();
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @PostMapping("/get-file-list")
    @ResponseBody
    public List<String> getFileList(@RequestParam String compressedFile) {
    	List<String> fileList = fileUploadService.getFileList(compressedFile);
    	return  fileList;
    }
}
```

**创建Thymeleaf模板：** 创建一个Thymeleaf模板（例如，`src/main/resources/templates/index.html`）来显示上传的文件列表和下载链接。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>文件上传和解压示例</title>
    <!-- 引入Bootstrap样式 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>文件上传和解压示例</h1>

        <!-- 显示压缩文件列表 -->
        <ul th:if="${compressedFiles.size() > 0}" class="list-group">
            <li th:each="compressedFile : ${compressedFiles}" class="list-group-item">
                <a href="#" th:attr="data-compressed-file=${compressedFile}" th:onclick="getFileList(this)">
                    <span class="glyphicon glyphicon-folder-close"></span>
                    <span th:text="${compressedFile}"></span>
                </a>
            </li>
        </ul>

        <!-- 显示文件列表 -->
        <ul th:id="fileList" class="list-group">
            <!-- 文件列表会在点击压缩文件后由JavaScript填充 -->
        </ul>
    </div>

    <!-- 文件上传表单 -->
    <div class="container mt-4">
        <form method="post" enctype="multipart/form-data" action="/upload">
            <div class="mb-3">
                <label for="file" class="form-label">选择压缩文件</label>
                <input type="file" class="form-control" id="file" name="file" accept=".zip,.rar"/>
            </div>
            <button type="submit" class="btn btn-primary">上传文件</button>
        </form>
    </div>

    <div th:if="${message}" class="container mt-4 alert alert-success" th:text="${message}"></div>
    <div th:if="${error}" class="container mt-4 alert alert-danger" th:text="${error}"></div>

    <!-- JavaScript函数来获取文件列表 -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script>

        function getFileList(element) {
        	 var compressedFile = element.getAttribute("data-compressed-file");
            $.ajax({
                url: '/get-file-list',
                type: 'POST',
                data: {compressedFile: compressedFile},
                success: function (data) {
                    var fileList = $('#fileList');
                    fileList.empty();
                    $.each(data, function (index, file) {
                        fileList.append('<li class="list-group-item"><a href="/download?fileName=' + file + '">' + file + '</a></li>');
                    });
                },
                error: function () {
                    alert('无法获取文件列表。');
                }
            });
        }
    </script>
</body>
</html>
```

以上是一个完整的示例，它实现了文件上传、解压、Thymeleaf模板和Bootstrap样式。

启动 Spring Boot 应用后，访问 http://localhost:8080/ ，显示上传按钮，选择要上传的压缩文件，上传成功后显示在列表中，点击列表中的压缩文件，会显示压缩文件中的详细文件列表。点击列表中文件名，可以下载该文件。