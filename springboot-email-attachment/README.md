 使用 Spring Boot + Thymeleaf 模板发送带有文件附件的电子邮件功能

在 Spring Boot 中发送带有文件附件的电子邮件涉及到以下几个步骤：配置邮件发送、创建 Thymeleaf 模板、生成邮件内容并添加附件。我会为你提供一个完整的示例，包括一个使用 Thymeleaf 的模板引擎来创建邮件内容的例子，同时提供中文描述。以下是详细的步骤：

**设置项目依赖**

首先在 Spring Boot 项目的`pom.xml`文件中添加所需的依赖项：

```xml
<dependencies>
    <!-- Spring Boot Starter Web (or your desired starter) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Starter Mail -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>

```

**配置邮件发送**

首先，你需要在 `application.properties`文件中配置邮件发送相关的属性，如邮件服务器主机、端口、认证信息等。

```properties
# 邮件配置
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=your-username
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**创建 Thymeleaf 模板**

在 `src/main/resources/templates` 目录下创建一个名为 `email-template.html` 的 Thymeleaf 模板文件。这个模板将会被用于生成邮件的内容。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
</head>
<body>
    <h1>您好，[[${username}]]！</h1>
    <p>以下是您的附件：</p>
    <p th:each="attachment : ${attachments}">
        <a th:href="${'cid:' + attachment.contentId}" th:text="${attachment.name}"></a>
    </p>
</body>
</html>
```

**创建附件类**

创建一个用于封装附件信息的类。

```java
package com.icoderoad.example.attachment.entity;

import lombok.Data;

@Data
public class Attachment {

    private String name;
    private String contentId;
    private byte[] data;
    private String contentType;

}
```

**创建邮件发送服务**

创建一个邮件发送服务类，用于实际发送邮件。

```java
package com.icoderoad.example.attachment.service;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.icoderoad.example.attachment.entity.Attachment;

@Service
public class EmailService {
	
	@Value("${spring.mail.username}")
	private String from;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendEmailWithAttachment(String to, String subject, String username, List<Attachment> attachments) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("attachments", attachments);
        String content = templateEngine.process("email-template", context);

        helper.setText(content, true);

        for (Attachment attachment : attachments) {
              helper.addAttachment(attachment.getName(), new ByteArrayResource(attachment.getData()), attachment.getContentType());
        }

        javaMailSender.send(message);
    }
}
```

**创建 Controller**

创建一个控制器类，用于接收请求并调用邮件发送服务。

```java
package com.icoderoad.example.attachment.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.icoderoad.example.attachment.entity.Attachment;
import com.icoderoad.example.attachment.service.EmailService;

@Controller
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/")
    public String showForm() {
        return "email-form";
    }

    @PostMapping("/send-email")
    public String sendEmail(@RequestParam("to") String to,
                            @RequestParam("subject") String subject,
                            @RequestParam("username") String username,
                            @RequestParam("file") MultipartFile file,
                            Model model
    		) throws IOException, MessagingException {
        List<Attachment> attachments = new ArrayList<>();
        if (!file.isEmpty()) {
            Attachment attachment = new Attachment();
            attachment.setName(file.getOriginalFilename());
            attachment.setContentId("attachment_" + System.currentTimeMillis());
            attachment.setData(file.getBytes());
            attachment.setContentType(file.getContentType());
            attachments.add(attachment);
        }

        emailService.sendEmailWithAttachment(to, subject, username, attachments);
        model.addAttribute("successMessage", "邮件已成功发送！");
        return "email-form";
    }
}
```

**步骤 6：创建 Thymeleaf 视图**

在 `src/main/resources/templates` 目录下创建一个名为 `email-form.html` 的 Thymeleaf 模板文件，用于展示邮件发送的表单。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title>发送邮件</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
 <div class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-body">
					    <h2>发送带附件的邮件</h2>
					    <div th:if="${successMessage}" class="alert alert-success" role="alert">
                            发送成功！
                        </div>
					    <form th:action="@{/send-email}" method="post" enctype="multipart/form-data">
					    	<div class="form-group">
						        <label for="to">收件人：</label>
						        <input type="text" class="form-control" id="to" name="to" required/><br/>
					        </div>
					        <div class="form-group">
						        <label for="subject">主题：</label>
						        <input type="text" class="form-control" id="subject" name="subject" required/><br/>
					        </div>
					        <div class="form-group">
						        <label for="username">用户名：</label>
						        <input type="text" class="form-control" id="username" name="username" required/><br/>
					        </div>
					        <div class="form-group">
						        <label for="file">附件：</label>
						        <input type="file"  class="form-control-file" id="file" name="file"/><br/>
					        </div>
					        <button type="submit" class="btn btn-primary">发送</button>
					    </form>
					</div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

确保你的 Spring Boot 应用的依赖中包含了 Thymeleaf、Spring Boot Starter Mail 等必要的依赖。