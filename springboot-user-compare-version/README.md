使用 SpringBoot+Interceptor + Mongodb 实现用户信息变化版本比较功能

实现Spring Boot应用程序，使用拦截器来记录用户信息的变化，并将这些信息存储在MongoDB中，然后创建一个Thymeleaf视图来展示用户列表、添加用户和编辑用户信息的功能，以及比较不同版本用户信息的功能。以下是一个简单的实现示例，包含了注释说明。

**创建Spring Boot应用程序**

首先我们需要 安装docker-compose，不同系统请参考官方文档：https://docs.docker.com/compose/install/

centos7安装命令

```sh
sudo curl -L "https://github.com/docker/compose/releases/download/1.23.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

创建 docker-compose.yml 文件

```yaml
version: '2.1'
services:
  mongo:
    image: "mongo:4.0-xenial"
    command: --replSet rs0 --smallfiles --oplogSize 128
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=mongouser
      - MONGO_INITDB_ROOT_PASSWORD=mongopw
```

在docker-compose.yml 文件相同目录执行 `docker-compose up -d`以后台模式启动docker-compose。

执行命令 docker-compose exec mongo /usr/bin/mongo -u mongouser -p mongopw 进行 mongodb 命令行，执行以下命令，创建登陆用户。

```sh
rs.initiate();
rs.status();
use mgdb
db.createUser({    
user: "mongouser",
pwd: "mongopw",
roles:[{role: "dbOwner" , db:"mgdb"}]
})
```

初始化用户数据

```
db.users.insertMany([
{
    userId: 100,
    username: "admin",
    email: "admin@icoderoad.com"
  },
  {
    userId: 101,
    username: "user1",
    email: "user1@icoderoad.com"
  },
  {
    userId: 102,
    username: "user2",
    email: "user2@icoderoad.com"
  }
]);
```

首先，创建一个Spring Boot项目并添加所需的依赖。在`pom.xml`文件中添加以下依赖：

```xml
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

<!-- Spring Data MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.12.0</version> 
</dependency>
```

**application.properties (应用程序配置文件)**

```properties
# Spring Boot应用程序配置
spring.application.name=UserComparisonApp
server.port=8080

# MongoDB连接配置
spring.data.mongodb.database=mgdb
spring.data.mongodb.uri=mongodb:///mongouser:mongopw@localhost:27017/mgdb

# Thymeleaf模板配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false

# 日志配置（可选）
# logging.level.org.springframework=INFO
# logging.level.com.yourcompany=DEBUG
```

请根据您的项目要求和环境调整这些配置文件。

**创建实体类**

创建一个用户信息版本的实体类

**UserVersion.java (用户版本实体类)**

```java
package com.icoderoad.example.userversion.entity;

import java.util.Date;

import lombok.Data;

@Data
public class UserVersion {
    private Date timestamp;
    private String username;
    private String email;
}
```

创建一个用户信息的实体类，以便将其存储在MongoDB中。

```java
package com.icoderoad.example.userversion.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String _id;
    private String userId;
    private String username;
    private String email;
    private List<UserVersion> versionHistory = new ArrayList<>();

    public void addToVersionHistory(UserVersion version) {
        this.versionHistory.add(version);
    }

}
```

**UserRepository.java (MongoDB仓库接口)**

```java
package com.icoderoad.example.userversion.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.icoderoad.example.userversion.entity.User;

public interface UserRepository extends MongoRepository<User, String> {
	 // 按userName查找用户的方法
    User findByUsername(String userName);
}
```

**创建拦截器UserChangeInterceptor** 

创建一个拦截器来记录用户信息的变化。拦截器可以捕获请求和响应，并将变化的用户信息存储在MongoDB中。

```java
package com.icoderoad.example.userversion.interceptor;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.icoderoad.example.userversion.entity.User;
import com.icoderoad.example.userversion.entity.UserVersion;
import com.icoderoad.example.userversion.repository.UserRepository;

@Component
public class UserChangeInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository; // UserRepository是用于MongoDB操作的接口

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在请求处理前记录用户信息的变化，存储到MongoDB中的历史版本

        // 1. 获取当前用户信息
        String userName = getCurrentUserName(); // 实现获取当前用户ID的逻辑
        User currentUser = userRepository.findByUsername(userName);
        if( currentUser == null ) {
        	  currentUser = new User();
        	  currentUser.setUsername("admin");
        	  currentUser.setEmail("admin@icoderoad.com");
        }
        createUserVersion(currentUser);

        return true;
    }

    private String getCurrentUserName() {
        // 实现获取当前用户ID的逻辑，可以使用Spring Security或其他方式
        // 返回当前用户的唯一标识符
        return "admin"; // 临时示例用户ID
    }
    
    private boolean isUserInfoChanged(User user, UserVersion version) {
        // 在这里添加逻辑来比较用户信息是否发生更改
        return !user.getUsername().equals(version.getUsername())
            || !user.getEmail().equals(version.getEmail())
           ;
    }
    
    private void createUserVersion(User user) {
        // 获取用户的版本历史
        List<UserVersion> versions = user.getVersionHistory();

        if (versions.isEmpty() || isUserInfoChanged(user, versions.get(versions.size() - 1))) {
            // 仅在用户信息发生更改或无版本历史记录时创建新的用户版本
            UserVersion newUserVersion = new UserVersion();
            newUserVersion.setUsername(user.getUsername());
            newUserVersion.setEmail(user.getEmail());
            // 设置其他版本信息字段

            // 将新版本添加到用户的版本历史
            versions.add(newUserVersion);

            // 保存用户对象以更新版本历史
            userRepository.save(user);
        }
    }
}
```

**配置拦截器**

配置拦截器以捕获用户信息的变化。在`WebMvcConfig.java`中添加以下配置：

```java
package com.icoderoad.example.userversion.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.icoderoad.example.userversion.interceptor.UserChangeInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private UserChangeInterceptor userChangeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userChangeInterceptor);
    }
}
```



**创建控制器**

创建控制器来处理用户的请求，并在需要时调用MongoDB操作来存储和检索用户信息。

```java
package com.icoderoad.example.userversion.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.userversion.entity.User;
import com.icoderoad.example.userversion.entity.UserVersion;
import com.icoderoad.example.userversion.interceptor.UserChangeInterceptor;
import com.icoderoad.example.userversion.repository.UserRepository;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

@Controller
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserChangeInterceptor userChangeInterceptor;

	@GetMapping("/")
	public String userList(Model model) {
		List<User> users = userRepository.findAll();
		model.addAttribute("users", users);
		return "user/users";
	}

	@GetMapping("/add")
	public String addUserForm(Model model) {
		model.addAttribute("user", new User());
		return "user/add";
	}

	@PostMapping("/save")
	public String saveUser(@ModelAttribute User user) {
		// 保存用户信息到MongoDB
		createUserVersion(user);
		userRepository.save(user);
		return "redirect:/";
	}

	@GetMapping("/edit/{id}")
	public String editUserForm(@PathVariable String id, Model model) {
		User user = userRepository.findById(id).orElse(new User());
		model.addAttribute("user", user);
		return "user/edit";
	}

	@PostMapping("/update")
	public String updateUser(@RequestParam String id, @ModelAttribute User user) {
		user.set_id(id);
		User mUser = userRepository.findById(id).orElse(new User());
		user.setVersionHistory(mUser.getVersionHistory());
		// 更新用户信息到MongoDB
		createUserVersion(user);
		userRepository.save(user);
		return "redirect:/";
	}

	@GetMapping("/compare")
	public String compareUsersForm(Model model) {
		List<User> users = userRepository.findAll();
		model.addAttribute("users", users);
		return "user/compare";
	}
	 @GetMapping("/compareVersions")
	    public String compareVersions(
	            @RequestParam String id,
	            @RequestParam(required = false) Integer version1Index,
	            @RequestParam(required = false) Integer version2Index,
	            Model model) {

	        // 获取用户和版本信息
	        User user = userRepository.findById(id).orElse(null);
	        model.addAttribute("userId", id);
	        if (user != null) {
	            List<UserVersion> versions = user.getVersionHistory();
	            model.addAttribute("userVersions", versions);
	            if( version1Index == null ) {
	            	version1Index = -1;
	            }
	            if( version2Index == null ) {
	            	version2Index = -1;
	            }
	            if (version1Index >= 0 && version1Index < versions.size()
	                    && version2Index >= 0 && version2Index < versions.size()) {
	                UserVersion version1 = versions.get(version1Index);
	                UserVersion version2 = versions.get(version2Index);

	                // 执行文本差异比较
	                String usernameDiff = compareStrings(version1.getUsername(), version2.getUsername());
	                String emailDiff = compareStrings(version1.getEmail(), version2.getEmail());

	                // 将比较结果存储在model中，以便在比较结果页面显示
	                model.addAttribute("usernameDiff", usernameDiff);
	                model.addAttribute("emailDiff", emailDiff);
	                model.addAttribute("comparisonResult", true);

	            }
	            
	            return "user/compareVersions"; // 返回比较结果页面
	        }

	        return "redirect:/users"; // 如果未找到用户或版本，重定向到用户列表页面
	    }

	    private String compareStrings(String str1, String str2) {
	        List<String> original = Arrays.asList(str1.split("\\n")); // 将字符串拆分为行
	        List<String> revised = Arrays.asList(str2.split("\\n")); // 将字符串拆分为行

	        // 创建Patch对象来存储差异
	        Patch<String> patch = DiffUtils.diff(original, revised);

	        StringBuilder diffOutput = new StringBuilder();
	        for (Delta<String> delta : patch.getDeltas()) {
	            String originalText = delta.getOriginal().getLines().toString();
	            String revisedText = delta.getRevised().getLines().toString();

	            // 添加高亮样式以区分新增和删除部分
	            diffOutput.append("<span class=\"deleted\">").append(originalText).append("</span>\n");
	            diffOutput.append("<span class=\"added\">").append(revisedText).append("</span>\n");
	        }

	        return diffOutput.toString();
	    }
	    
	    private boolean isUserInfoChanged(User user, UserVersion version) {
	        // 在这里添加逻辑来比较用户信息是否发生更改
	        return !user.getUsername().equals(version.getUsername())
	            || !user.getEmail().equals(version.getEmail())
	           ;
	    }
	    
	    private void createUserVersion(User user) {
	        // 获取用户的版本历史
	        List<UserVersion> versions = user.getVersionHistory();

	        if (versions.isEmpty() || isUserInfoChanged(user, versions.get(versions.size() - 1))) {
	            // 仅在用户信息发生更改或无版本历史记录时创建新的用户版本
	            UserVersion newUserVersion = new UserVersion();
	            newUserVersion.setUsername(user.getUsername());
	            newUserVersion.setEmail(user.getEmail());
	            // 设置其他版本信息字段

	            // 将新版本添加到用户的版本历史
	            versions.add(newUserVersion);

	            // 保存用户对象以更新版本历史
	            userRepository.save(user);
	        }
	    }
}
```

**创建Thymeleaf视图**

创建Thymeleaf视图来展示用户列表、添加用户和编辑用户信息的功能，以及比较不同版本用户信息的功能。

**用户列表页面 (`users.html`)**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>用户列表</title>
    <!-- 引入Bootstrap CSS文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>用户列表</h1>
        
        <!-- 使用Bootstrap表格样式 -->
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>用户ID</th>
                    <th>用户名</th>
                    <th>电子邮件</th>
                    <!-- 其他字段的中文描述 -->
                </tr>
            </thead>
            <tbody>
                <!-- 使用Thymeleaf迭代渲染用户列表 -->
                <tr th:each="user : ${users}">
                    <td th:text="${user.userId}"></td>
                    <td th:text="${user.username}"></td>
                    <td th:text="${user.email}"></td>
                   <td>
                        <a th:href="@{'/edit/' + ${user._id}}" class="btn btn-warning">编辑</a>
                        <a th:href="@{'/compareVersions?id=' + ${user._id}}" class="btn btn-info">版本比较</a>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- 添加用户按钮 -->
        <a class="btn btn-primary" th:href="@{/add}">添加用户</a>
    </div>
</body>
</html>
```

**添加用户页面 (`add.html`)**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>添加用户</title>
    <!-- 引入Bootstrap CSS文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>添加用户</h1>
        
        <!-- 使用Bootstrap表单样式和中文字段描述 -->
        <form th:action="@{/save}" method="post">
            <div class="form-group">
                <label for="userId">用户ID</label>
                <input type="text" class="form-control" id="userId" name="userId" required="required">
            </div>
            <div class="form-group">
                <label for="username">用户名</label>
                <input type="text" class="form-control" id="username" name="username" required="required">
            </div>
            <div class="form-group">
                <label for="email">电子邮件</label>
                <input type="email" class="form-control" id="email" name="email" required="required">
            </div>
            <!-- 其他字段的中文描述和表单输入 -->
            <button type="submit" class="btn btn-primary">保存</button>
        </form>
        
        <!-- 返回按钮 -->
        <a class="btn btn-secondary" th:href="@{/users}">返回</a>
    </div>
</body>
</html>
```

**编辑用户页面 (`edit.html`)**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>编辑用户</title>
    <!-- 引入Bootstrap CSS文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <h1>编辑用户</h1>
        
        <!-- 使用Bootstrap表单样式和中文字段描述 -->
        <form th:action="@{/update}" method="post">
        	 <!-- 隐藏的 _id 输入字段 -->
    		<input type="hidden" th:value="${user._id}"  name="id" />
            <div class="form-group">
                <label for="userId">用户ID</label>
                <input type="text" class="form-control" id="userId" name="userId" th:value="${user.userId}" required="required">
            </div>
            <div class="form-group">
                <label for="username">用户名</label>
                <input type="text" class="form-control" id="username" name="username" th:value="${user.username}" required="required">
            </div>
            <div class="form-group">
                <label for="email">电子邮件</label>
                <input type="email" class="form-control" id="email" name="email" th:value="${user.email}" required="required">
            </div>
            <!-- 其他字段的中文描述和表单输入 -->
            <button type="submit" class="btn btn-primary">更新</button>
        </form>
        
        <!-- 返回按钮 -->
        <a class="btn btn-secondary" th:href="@{/users}">返回</a>
    </div>
</body>
</html>
```

**比较用户页面 (`compareVersions`)**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>比较用户版本</title>
    <!-- 引入Bootstrap CSS文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
    .added {
		    background-color: #d9ffdb; /* 高亮显示新增部分的背景色 */
	}
		
	.deleted {
		    background-color: #ffd9d9; /* 高亮显示删除部分的背景色 */
	}
    
    </style>
</head>
<body>
    <div class="container">
        <h1>比较用户版本</h1>
        
        <h2>选择要比较的版本：</h2>
        <form th:action="@{/compareVersions}" method="get">
            <!-- 用户ID参数 -->
            <input type="hidden" name="id" th:value="${userId}" />

            <!-- 版本选择下拉菜单 -->
            <select name="version1Index" id="version1">
                <option value="-1">选择版本 1</option>
                <!-- 使用Thymeleaf迭代渲染用户版本列表 -->
                <option th:each="version, versionIndex : ${userVersions}" 
                        th:value="${versionIndex.index}" 
                        th:text="${'版本 ' + (versionIndex.index+1)}"></option>
            </select>

            <select name="version2Index" id="version2">
                <option value="-1">选择版本 2</option>
                <!-- 使用Thymeleaf迭代渲染用户版本列表 -->
                <option th:each="version, versionIndex : ${userVersions}" 
                        th:value="${versionIndex.index}" 
                        th:text="${'版本 ' + (versionIndex.index+1)}"></option>
            </select>

            <button type="submit" class="btn btn-primary">比较</button>
        </form>

        <!-- 显示比较结果 -->
        <div th:if="${comparisonResult != null}">
            <h2>比较结果：</h2>
            <p>用户名：</p>
            <pre th:utext="${usernameDiff}"></pre>

            <p>电子邮件：</p>
            <pre th:utext="${emailDiff}"></pre>

            <!-- 其他字段的比较结果 -->
        </div>

        <!-- 返回按钮 -->
        <a class="btn btn-secondary" th:href="@{/}">返回</a>
    </div>
</body>
</html>
```

**配置拦截器**

配置拦截器以捕获用户信息的变化。在`WebMvcConfig.java`中添加以下配置：

```
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private UserChangeInterceptor userChangeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userChangeInterceptor);
    }
}
```

以上代码提供了一个基本的实现示例，涵盖了用户信息的记录、存储、展示和比较功能。根据实际需求，可以进一步完善代码。