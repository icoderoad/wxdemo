使用 springBoot + JDBC + mysql 实现员工的分页显示及增删改查功能

**数据表 DDL语句**

```sql
CREATE TABLE employee (
  emp_id VARCHAR(10) NOT NULL,
  emp_name VARCHAR(100) NOT NULL
);
```

**Pom.xml依赖**

```xml
			 <!-- Spring Boot Starter Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- MySQL Connector -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
```

**`application.properties`属性配置**

```properties
# 配置应用程序的端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_db_name
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 设置Thymeleaf模板文件的缓存，开发阶段可以设置为false
spring.thymeleaf.cache=false

# 设置Thymeleaf模板前缀和后缀
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
```

确保替换your_db_name、your_username和your_password为实际的数据库名称、用户名和密码。

**实体类 Employee.java：**

```java
package com.icoderoad.example.employee.entity;

import lombok.Data;

@Data
public class Employee {
    private String empId;
    private String empName;
}
```

**员工数据访问层 EmployeeRepository.java：**

```java
package com.icoderoad.example.employee.repository;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.employee.entity.Employee;

@Repository
public class EmployeeRepository {
    private final JdbcTemplate jdbcTemplate;

    public EmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("deprecation")
	public List<Employee> findAllEmployees(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT * FROM employee LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new Object[]{pageSize, offset}, new BeanPropertyRowMapper<>(Employee.class));
    }

    public int countEmployees() {
        String sql = "SELECT COUNT(*) FROM employee";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public void addEmployee(Employee employee) {
        String sql = "INSERT INTO employee (emp_id, emp_name) VALUES (?, ?)";
        jdbcTemplate.update(sql, employee.getEmpId(), employee.getEmpName());
    }

    public Employee findEmployeeById(String empId) {
        String sql = "SELECT * FROM employee WHERE emp_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{empId}, new BeanPropertyRowMapper<>(Employee.class));
    }

    public void updateEmployee(Employee employee) {
        String sql = "UPDATE employee SET emp_name = ? WHERE emp_id = ?";
        jdbcTemplate.update(sql, employee.getEmpName(), employee.getEmpId());
    }

    public void deleteEmployee(String empId) {
        String sql = "DELETE FROM employee WHERE emp_id = ?";
        jdbcTemplate.update(sql, empId);
    }
}
```

**控制器 EmployeeController.java：**

```java
package com.icoderoad.example.employee.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.employee.entity.Employee;
import com.icoderoad.example.employee.repository.EmployeeRepository;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/list")
    public String listEmployees(Model model,
                                 @RequestParam(name = "page", defaultValue = "1") int page,
                                 @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        int totalEmployees = employeeRepository.countEmployees();
        int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages!=0) {
            page = totalPages;
        }

        model.addAttribute("employees", employeeRepository.findAllEmployees(page, pageSize));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "employee/list";
    }

    @GetMapping("/add")
    public String addEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee/add";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute Employee employee) {
        employeeRepository.addEmployee(employee);
        return "redirect:/employee/list";
    }

    @GetMapping("/edit/{empId}")
    public String editEmployeeForm(@PathVariable String empId, Model model) {
        Employee employee = employeeRepository.findEmployeeById(empId);
        model.addAttribute("employee", employee);
        return "employee/edit";
    }

    @PostMapping("/edit/{empId}")
    public String editEmployee(@PathVariable String empId, @ModelAttribute Employee employee) {
        employeeRepository.updateEmployee(employee);
        return "redirect:/employee/list";
    }

    @GetMapping("/delete/{empId}")
    public String deleteEmployee(@PathVariable String empId) {
        employeeRepository.deleteEmployee(empId);
        return "redirect:/employee/list";
    }
}
```

**`employee/list.html`（员工列表页面）：**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>员工列表</title>
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/bootstrap/5.0.0/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h1>员工列表</h1>
    <a th:href="@{/employee/add}" class="btn btn-primary">添加员工</a>
    
    <table class="table table-bordered">
        <thead>
        <tr>
            <th>员工ID</th>
            <th>员工姓名</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="employee : ${employees}">
            <td th:text="${employee.empId}">员工ID</td>
            <td th:text="${employee.empName}">员工姓名</td>
            <td>
                <a th:href="@{/employee/edit/{empId}(empId=${employee.empId})}">编辑</a>
                <a th:href="@{/employee/delete/{empId}(empId=${employee.empId})}">删除</a>
            </td>
        </tr>
        </tbody>
    </table>
    <nav aria-label="Page navigation">
        <ul class="pagination">
            <li th:class="${currentPage == 1} ? 'page-item disabled' : 'page-item'">
                <a th:href="@{${currentPage == 1} ? '#' : '/employee/list?page=1'}" class="page-link">首页</a>
            </li>
            <li th:class="${currentPage == 1} ? 'page-item disabled' : 'page-item'">
                <a th:href="${currentPage == 1 ? '#' : '/employee/list?page=' + (currentPage - 1)}" class="page-link">上一页</a>
            </li>
            <li th:class="${currentPage == totalPages} ? 'page-item disabled' : 'page-item'">
                <a th:href="${currentPage == totalPages ? '#' : '/employee/list?page=' + (currentPage + 1)}" class="page-link">下一页</a>
            </li>
            <li th:class="${currentPage == totalPages} ? 'page-item disabled' : 'page-item'">
                <a th:href="${currentPage == totalPages ? '#' : '/employee/list?page=' + totalPages}" class="page-link">末页</a>
            </li>
        </ul>
    </nav>
</div>
</body>
</html>
```

**`employee/add.html`（添加员工页面）：**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>添加员工</title>
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/bootstrap/5.0.0/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h1>添加员工</h1>
    <form method="post" th:action="@{/employee/add}">
        <div class="form-group">
            <label for="empId">员工ID</label>
            <input type="text" class="form-control" id="empId" name="empId" required>
        </div>
        <div class="form-group">
            <label for="empName">员工姓名</label>
            <input type="text" class="form-control" id="empName" name="empName" required>
        </div>
        <button type="submit" class="btn btn-primary">添加</button>
    </form>
</div>
</body>
</html>
```

**`employee/edit.html`（编辑员工页面）：**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>编辑员工</title>
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/bootstrap/5.0.0/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h1>编辑员工</h1>
    <form method="post" th:action="@{/employee/edit/{empId}(empId=${employee.empId})}">
        <div class="form-group">
            <label for="empId">员工ID</label>
            <input type="text" class="form-control" id="empId" name="empId" th:value="${employee.empId}" readonly>
        </div>
        <div class="form-group">
            <label for="empName">员工姓名</label>
            <input type="text" class="form-control" id="empName" name="empName" th:value="${employee.empName}" required>
        </div>
        <button type="submit" class="btn btn-primary">保存</button>
    </form>
</div>
</body>
</html>
```

这些HTML模板文件包含了中文描述的员工列表、添加和编辑页面。它们使用了Thymeleaf标签来渲染数据和处理表单提交。同时，使用了Bootstrap样式来美化页面。确保将这些模板文件放置在`src/main/resources/templates/employee`目录下，以便Thymeleaf能够正确渲染它们。

1. 启动应用程序：创建一个Spring Boot应用程序的入口类，并使用@SpringBootApplication注解启动应用程序。

现在，我们可以通过访问http://localhost:8080/employee/list来访问员工管理功能，实现员工的分页显示，增删改查等相关功能。