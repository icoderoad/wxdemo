Springboot + Thymeleaf + Bootstrap 实现人脸检测功能

实现人脸检测功能通常涉及多个技术和步骤，包括图像处理、机器学习/深度学习模型的使用，以及前端界面的设计。在这里，我将为大家提供一个简单的Spring Boot应用程序，该应用程序使用Thymeleaf和Bootstrap创建一个Web界面，用户可以上传图像并使用OpenCV进行人脸检测。

注意，这只是一个基本的示例，实际的人脸检测系统需要更复杂的模型和算法。

**准备项目**

首先，我们需要创建一个Spring Boot项目，包含Thymeleaf和Bootstrap依赖。可以使用Spring Initializer（https://start.spring.io/）来创建一个包含这些依赖的项目。

**添加依赖**

在项目的`pom.xml`文件中，添加以下依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.openpnp</groupId>
  <artifactId>opencv</artifactId>
  <version>4.7.0-0</version>
</dependency>
```

**配置 application.properties 属性**

```properties
# 配置上传目录
upload.directory=/path/to/upload/directory

# 配置输出目录
output.directory=/path/to/output/directory
#配置静态资源图片访问目录
spring.resources.static-locations=file:/path/to/output/directory

# Thymeleaf 配置信息
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

将`/path/to/upload/directory`和`/path/to/output/directory`替换为希望使用的实际目录路径。

**创建Controller**

创建一个Spring Boot控制器，用于处理文件上传和人脸检测。以下是一个示例控制器：

```java
package com.icoderoad.example.facedetection.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import nu.pattern.OpenCV;

@Controller
public class FaceDetectionController {

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Value("${output.directory}")
    private String outputDirectory;

    static {
    	OpenCV.loadLocally();
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/detect")
    public String detectFaces(@RequestParam("file") MultipartFile file, Model model) {
        if (!file.isEmpty()) {
            try {
                String fileName = file.getOriginalFilename();
                String filePath = uploadDirectory + "/" + fileName;
                File uploadDir = new File(uploadDirectory);
                if( !uploadDir.exists() ) {
                	uploadDir.mkdirs();
                }
                // 保存上传的文件
                byte[] bytes = file.getBytes();
                Path path = Paths.get(filePath);
                Files.write(path, bytes);

                // 加载OpenCV级联分类器
             // 使用ClassLoader加载级联分类器文件
                Resource resource = new ClassPathResource("model/haarcascade_frontalface_default.xml");
                File cascadeFile = resource.getFile();
                CascadeClassifier faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());


                // 读取上传的图像文件
                Mat image = Imgcodecs.imread(filePath);

                // 在图像中检测人脸
                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(image, faceDetections);

                // 在图像上绘制检测到的人脸
                for (Rect rect : faceDetections.toArray()) {
                    Imgproc.rectangle(image, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
                }
                
                File outputDir = new File(outputDirectory);
                if( !outputDir.exists() ) {
                	outputDir.mkdirs();
                }
                
                // 保存带有人脸检测结果的图像
                String outputFilePath = outputDirectory + "/" + fileName;
                Imgcodecs.imwrite(outputFilePath, image);

                model.addAttribute("resultImage", "/images/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "result";
    }
}
```

上面的控制器使用OpenCV进行人脸检测，我们已经将OpenCV库添加到项目依赖中。它还保存了检测到人脸的图像，并将结果文件的路径传递给Thymeleaf模板。

**创建Controller来提供图像文件**：

创建一个新的Spring Boot控制器，用于提供生成的人脸检测结果图像。以下是一个示例控制器：

```java
package com.icoderoad.example.facedetection.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/images")
public class ImageController {

	@Value("${output.directory}")
	private String outputDirectory;

	@GetMapping("/{imageName:.+}")
	public ResponseEntity<Resource> getImage(@PathVariable String imageName) throws IOException {
		Path imagePath = Paths.get(outputDirectory).resolve(imageName);
		Resource resource = new FileSystemResource(imagePath.toFile());

		if (resource.exists() && resource.isReadable()) {
			return ResponseEntity.ok().contentLength(resource.contentLength()).contentType(MediaType.IMAGE_JPEG) // 适配图像类型
					.body(resource);
		} else {
			// 处理资源不存在的情况，例如返回404
			return ResponseEntity.notFound().build();
		}
	}
}
```

**创建Thymeleaf模板**

创建Thymeleaf模板文件，用于处理文件上传和显示人脸检测结果。

`src/main/resources/templates/index.html`：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>人脸检测</title>
    <!-- 添加Bootstrap样式 -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<div class="container mt-5">
    <h2>人脸检测</h2>
    <form action="/detect" method="post" enctype="multipart/form-data">
        <div class="form-group">
            <input type="file" name="file" class="form-control-file">
        </div>
        <button type="submit" class="btn btn-primary">上传并检测</button>
    </form>
</div>
</body>
</html>
```

`src/main/resources/templates/result.html`：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>人脸检测结果</title>
    <!-- 添加Bootstrap样式 -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<div class="container mt-5">
    <h2>人脸检测结果</h2>
    <img th:src="${resultImage}" alt="Result Image" class="img-fluid">
</div>
</body>
</html>
```

**配置OpenCV**

在控制器中，我们使用了OpenCV进行人脸检测，所以需要确保OpenCV库已正确加载。在示例中，我们使用了静态块来加载OpenCV库。

**配置OpenCV级联分类器**

在示例中，我们使用了OpenCV的级联分类器（Cascade Classifier）来进行人脸检测。你需要下载一个Haar级联分类器XML文件（例如，`haarcascade_frontalface_default.xml`），并在控制器中加载它。你可以在OpenCV官方网站上找到这些XML文件。

** 运行应用程序**

完成上述步骤后，运行Spring Boot应用程序，并在浏览器中访问 `http://localhost:8080/` 来上传图像并进行人脸检测。