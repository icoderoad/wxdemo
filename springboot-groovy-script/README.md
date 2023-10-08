Springboot +  Groovy + Thymeleaf + Bootstrap  实现动态配置脚本功能

以下是一个大致的实现步骤和代码示例，包括pom.xml依赖和属性配置以及核心代码的中文注释：

创建一个Spring Boot项目，确保在pom.xml中添加必要的依赖：

```xml
<!-- Spring Boot依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>

<!-- Thymeleaf依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Groovy依赖 -->
<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy</artifactId>
</dependency>

<!-- Bootstrap依赖 -->
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>bootstrap</artifactId>
    <version>4.5.3</version> <!-- 使用适当的版本 -->
</dependency>
```

在 src/main/resources 目录下创建目录  scripts，创建一个Groovy脚本  DefaultScript.groovy 来处理动态配置，比如获取系统默认时间和星期几。

```groovy
// DefaultScript.groovy
import java.text.SimpleDateFormat
import java.util.Date

def getCurrentTime() {
    def dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return dateFormat.format(new Date())
}

def getCurrentDayOfWeek() {
    def dateFormat = new SimpleDateFormat("EEEE")
    return dateFormat.format(new Date())
}
```

在Spring Boot中配置Groovy脚本的路径，以便动态加载。在application.properties中添加：

```properties
# Groovy脚本路径
groovy.script.path=classpath:scripts/

# Thymeleaf模板配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

创建一个Controller处理页面请求，并使用Thymeleaf和Bootstrap创建一个简单的前端界面。

```java
package com.icoderoad.example.groovy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.groovy.util.GroovyScriptExecutor;

@Controller
public class ScriptController {

    @GetMapping("/")
    public String index(Model model) throws Exception {
        // 此处调用Groovy脚本来获取默认时间和星期几
        String currentTime = GroovyScriptExecutor.executeScript("DefaultScript.groovy", "getCurrentTime");
        String currentDayOfWeek = GroovyScriptExecutor.executeScript("DefaultScript.groovy", "getCurrentDayOfWeek");

        model.addAttribute("currentTime", currentTime);
        model.addAttribute("currentDayOfWeek", currentDayOfWeek);

        return "index";
    }
}
```

创建一个Groovy脚本执行器类来加载和执行Groovy脚本。

```java
package com.icoderoad.example.groovy.util;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

public class GroovyScriptExecutor {

    public static String executeScript(String scriptName, String methodName) {
        GroovyClassLoader loader = new GroovyClassLoader();
        try {
            Class groovyClass = loader.parseClass(
                GroovyScriptExecutor.class.getClassLoader().getResourceAsStream("scripts/" + scriptName), methodName
            );

            GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
            return (String) groovyObject.invokeMethod(methodName, null);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing script";
        }
    }
}
```

最后，创建一个Thymeleaf模板（index.html）来显示系统默认时间和星期几以及提供自定义脚本功能。

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>动态配置脚本功能</title>
    <!-- 导入Bootstrap样式 -->
    <link rel="stylesheet" href="/webjars/bootstrap/4.5.3/css/bootstrap.min.css">
</head>
<body>
<div class="container mt-5">
    <h1>系统默认时间：</h1>
    <p th:text="${currentTime}"></p>

    <h1>当前星期几：</h1>
    <p th:text="${currentDayOfWeek}"></p>

    <!-- 在此处添加自定义脚本功能 -->

</div>
</body>
</html>
```

启动 Spring Boot 应用后，访问 http://localhost:8080/，显示动态脚本功能页面 。