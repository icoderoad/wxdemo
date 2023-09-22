使用Springboot + Tesseract OCR引擎实现车牌自动识别功能

下载训练好的Tesseract模型文件（`.traineddata`文件），以支持车牌号码识别。从 GitHub仓库（https://github.com/tesseract-ocr/tessdata）以获取最新版本的 `.traineddata` 文件

也可以使用 https://raw.githubusercontent.com/tesseract-ocr/tessdata/main/chi_sim.traineddata 直接下载

将下载的 /chi_sim.traineddata  文件放置在项目的资源目录（通常是`src/main/resources`）下的一个子目录中，例如 `src/main/resources/tessdata`。

 `pom.xml` 配置文件

```xml
				<!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Thymeleaf and Bootstrap -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- Tesseract OCR -->
        <dependency>
            <groupId>net.sourceforge.tess4j</groupId>
            <artifactId>tess4j</artifactId>
            <version>5.8.0</version>
        </dependency>
```

`application.properties`文件配置

```properties
# Spring Boot应用端口配置（根据需要进行调整）
server.port=8080

# Tesseract OCR的数据路径配置，确保指向存放 .traineddata 文件的目录
tesseract.dataPath=classpath:/tessdata/
```

配置Tesseract OCR类：

```java
package com.icoderoad.example.numberplate.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Configuration
public class TesseractConfig {

    @Value("${tesseract.dataPath}")
    private String tessDataPath;

    @Bean
    public Tesseract tesseract() throws TesseractException {
    	
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("chi_sim"); // 设置语言

        return tesseract;
    }
}
```

在上述配置中，我们使用`Tesseract`类来配置Tesseract OCR引擎，并指定 `.traineddata` 文件的位置。

Controller控制器类：

创建Spring Boot控制器，处理上传和识别请求，并在上传后重定向到识别结果页面

```java
package com.icoderoad.example.numberplate.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;

@Controller
public class LicensePlateRecognitionController {

    @Autowired
    private Tesseract tesseract;
    
     @GetMapping("/upload")
    public String showUploadForm() {
        return "upload";
    }
    
    @PostMapping("/recognize")
    public String recognizeLicensePlate(@RequestParam("imageFile") MultipartFile imageFile, Model model) {
        try {

             File convFile = convert(imageFile);
            String recognizedText = tesseract.doOCR(convFile);
            
            model.addAttribute("result", "识别结果：" + recognizedText);

            return "result";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
    
    public static File convert(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

}
```

HTML表单视图（upload.html）来实现车牌图像上传

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>上传车牌图像</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <h1>上传车牌图像</h1>
        <form action="/recognize" method="post" enctype="multipart/form-data">
            <div class="form-group">
                <label for="imageFile">选择要上传的图像：</label>
                <input type="file" class="form-control-file" id="imageFile" name="imageFile">
            </div>
            <button type="submit" class="btn btn-primary">上传并识别</button>
        </form>
    </div>
</body>
</html>
```

识别结果视图（result.html）来显示识别结果

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>车牌识别结果</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <h1>车牌识别结果</h1>
        <p th:text="${result}"></p>
        <a href="/upload" class="btn btn-primary">返回上传</a>
    </div>
</body>
</html>
```

启动应用 ，访问 http://localhost:8080/upload ，上传车牌图像使用Tesseract OCR进行车牌识别,最后将结果显示在识别结果页。



问题：Unsupported image format. May need to install JAI Image I/O package. 

解决：把 jai-imageio-core-1.4.0.jar(这是tess4j的依赖jar,一般你下载tess4j的工程里面就会有这个包)这个包单独复制了一个到re下的ext文件夹里面(我的路径E:\java\jdk8\jre\lib\ext)