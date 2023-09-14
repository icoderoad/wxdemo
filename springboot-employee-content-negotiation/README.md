使用 Springboot + JPA + Thymeleaf 通过内容协商显示 JSON 及 XML 员工列表功能

完整的解决方案代码如下，包括了创建表 DDL语句、pom.xml 依赖、application.properties 属性配置、创建员工表、数据初始化、配置内容协商Content Negotiation、创建REST接口、Thymeleaf视图、引入Bootstrap和jQuery等内容。

用于创建员工表的DDL语句：

```sql
CREATE TABLE cn_employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    salary DECIMAL(10, 2)
);
```

**pom.xml配置**

```xml
 		<!-- Spring Boot Web Starter for building web applications -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA Starter for working with relational databases -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL Database Driver -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
        </dependency>
```

**`application.properties` 配置文件**

```properties
# 应用程序端口配置
server.port=8080

# 数据库连接配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# 配置Thymeleaf视图文件位置（可选）
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

**创建实体类 `Employee.java`：**

```java
package com.icoderoad.example.employee.entity;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name="cn_employee")
public class Employee {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private BigDecimal salary;
}
```

**创建员工数据初始化类 `DataInitializer.java`：**

初始化10名员工数据

```java
package com.icoderoad.example.employee.init;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.icoderoad.example.employee.entity.Employee;
import com.icoderoad.example.employee.repository.EmployeeRepository;

@Component
public class DataInitializer implements ApplicationRunner {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public DataInitializer(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
    	long total = employeeRepository.count();
    	if( total<10 ) {
	        // 初始化10个员工数据
	        for (int i = 1; i <= 10; i++) {
	            Employee employee = new Employee();
	            employee.setName("员工" + i);
	            employee.setDescription("员工" + i + "的描述");
	            employee.setSalary(BigDecimal.valueOf(50000 + i * 1000));
	            employeeRepository.save(employee);
	        }
    	}
    }
}
```

**配置Content Negotiation**

 使用Spring Boot配置Content Negotiation，使应用能够根据URL扩展名或format返回JSON或XML格式的数据。

```java
package com.icoderoad.example.employee.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
	
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .defaultContentType(MediaType.APPLICATION_JSON) // 设置默认的媒体类型为JSON
            .favorParameter(true) // 启用通过请求参数来进行内容协商，例如"?format=json"
            .parameterName("format") // 设置请求参数的名称，默认是"format"
            .ignoreAcceptHeader(true) // 忽略请求头中的Accept字段
            .useJaf(false) // 不使用Java Activation Framework来确定媒体类型
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML)
            .mediaType("json", MediaType.APPLICATION_JSON);
    }
}
```

 上述代码中的各个配置项的含义如下：

- `defaultContentType(MediaType.APPLICATION_JSON)`：设置默认的媒体类型为JSON。如果请求不显式指定媒体类型，将使用这个默认类型。
- `favorPathExtension(true)`：启用通过文件扩展名来进行内容协商。例如，如果请求路径是"/employees.json"，则返回JSON数据。
- `favorParameter(false)`：不启用通过请求参数来进行内容协商。例如，不支持通过"?format=json"来指定媒体类型。
- `parameterName("format")`：设置请求参数的名称，用于指定希望的媒体类型。默认是"format"，可根据需求修改。
- `ignoreAcceptHeader(true)`：忽略请求头中的Accept字段。即使请求头中包含Accept字段，仍然会根据文件扩展名或参数来确定媒体类型。
- `useJaf(false)`：不使用Java Activation Framework来确定媒体类型。Java Activation Framework可以通过文件扩展名来确定媒体类型，但这里禁用了它。
- `mediaType("json", MediaType.APPLICATION_JSON)`：将扩展名"json"映射到JSON媒体类型。这表示如果请求的文件扩展名是".json"，将返回JSON数据。
- `mediaType("xml", MediaType.APPLICATION_XML)`：将扩展名"xml"映射到XML媒体类型。这表示如果请求的文件扩展名是".xml"，将返回XML数据。

**创建实体类的数据访问接口 `EmployeeRepository.java`：**

```java
package com.icoderoad.example.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.employee.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
```

**创建控制器类 `EmployeeController.java`：**

```java
package com.icoderoad.example.employee.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.employee.entity.Employee;
import com.icoderoad.example.employee.repository.EmployeeRepository;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<Employee> getAllEmployeesJson() {
        return employeeRepository.findAll();
    }
}
```

**创建控制器类 EmployeeViewController：**

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/view")
public class EmployeeViewController {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeViewController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getJsonView(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        return "employee-json";
    }

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getXmlView(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        return "employee-xml";
    }
}
```

**创建Thymeleaf视图文件 `employee-json.html`：**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>员工数据（JSON格式）</title>
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>员工数据（JSON格式）</h1>
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>姓名</th>
                    <th>简介</th>
                    <th>薪水</th>
                </tr>
            </thead>
            <tbody id="jsonEmployeeTable">
                <!-- 此处将通过jQuery异步加载员工数据 -->
            </tbody>
        </table>
    </div>
    <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script>
        $(document).ready(function() {
            $.get("/employees?format=json", function(data) {
                var employeeTable = $("#jsonEmployeeTable");
                $.each(data, function(index, employee) {
                    var row = "<tr>" +
                        "<td>" + employee.id + "</td>" +
                        "<td>" + employee.name + "</td>" +
                        "<td>" + employee.description + "</td>" +
                        "<td>" + employee.salary + "</td>" +
                        "</tr>";
                    employeeTable.append(row);
                });
            });
        });
    </script>
</body>
</html>
```

**创建Thymeleaf视图文件 `employee-xml.html`：**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>员工数据（XML格式）</title>
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>员工数据（XML格式）</h1>
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>姓名</th>
                    <th>简介</th>
                    <th>薪水</th>
                </tr>
            </thead>
            <tbody id="xmlEmployeeTable">
                <!-- 此处将通过jQuery异步加载员工数据 -->
            </tbody>
        </table>
    </div>
    <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script>
        $(document).ready(function() {
            $.get("/employees?format=xml", function(data) {
                 var employeeTable = $("#xmlEmployeeTable");
                 $(data).find("item").each(function() {
                     var employee = $(this);
                     var row = "<tr>" +
                         "<td>" + employee.children("id").text() + "</td>" +
                         "<td>" + employee.children("name").text() + "</td>" +
                         "<td>" + employee.children("description").text() + "</td>" +
                         "<td>" + employee.children("salary").text() + "</td>" +
                         "</tr>";
                     employeeTable.append(row);
                 });
            });
        });
    </script>
</body>
</html>
```

请将上述代码中的数据库连接信息、数据库名、用户名和密码替换为自己的实际配置。启动应用程序，我们可以通过访问http://localhost:8080/view/json，http://localhost:8080/view/xml来访问员工列表，查看json及xml不同格式接口页面显示效果。