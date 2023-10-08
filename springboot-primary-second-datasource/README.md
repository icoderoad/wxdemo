Springboot + Mysql  用户表 + Thymeleaf + Bootstrap 实现主从数据源配功能

为了实现Spring Boot项目中的动态数据源配置，用户表，Thymeleaf，Bootstrap以及两个数据源的初始化数据，你需要进行以下步骤。下面是一个简化的示例，提供了相关配置和代码注释：

在`pom.xml`中添加依赖：

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Starter Thymeleaf -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Spring Boot Starter Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Connector -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

在`application.properties`中配置数据源属性：

```properties
# 主数据源配置
spring.datasource.primary.url=jdbc:mysql://localhost:3306/db_primary
spring.datasource.primary.username=root
spring.datasource.primary.password=root
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

# 第二个数据源配置
spring.datasource.secondary.url=jdbc:mysql://localhost:3306/db_secondary
spring.datasource.secondary.username=root
spring.datasource.secondary.password=root
spring.datasource.secondary.driver-class-name=com.mysql.cj.jdbc.Driver
```

创建用户表的DDL语句，比如：

```sql
CREATE TABLE datasource_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);
```

数据源配置类

```java
package com.icoderoad.example.datasource.conf;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfiguration {

    @Bean(name = "primaryDataSource")
    @Qualifier("primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    @Primary
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "secondDataSource")
    @Qualifier("secondDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

主数据源配置类

```java
package com.icoderoad.example.datasource.conf;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Description: 主数据源配置
 * @date
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactoryPrimary",
                        transactionManagerRef = "transactionManagerPrimary",
                        basePackages = {"com.icoderoad.example.datasource.repository.primary"})
public class PrimaryDataSourceConfig {

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Autowired
    private HibernateProperties hibernateProperties;

    @Autowired
    private JpaProperties jpaProperties;

    @Primary
    @Bean(name = "entityManagerPrimary")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return entityManagerFactoryPrimary(builder).getObject().createEntityManager();
    }

    @Primary
    @Bean(name = "entityManagerFactoryPrimary")    //primary实体工厂
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryPrimary (EntityManagerFactoryBuilder builder) {
        return builder.dataSource(primaryDataSource)
                .properties(getHibernateProperties())
                .packages("com.icoderoad.example.datasource.entity.primary")     //换成你自己的实体类所在位置
                .persistenceUnit("primaryPersistenceUnit")
                .build();
    }

    @Primary
    @Bean(name = "transactionManagerPrimary")
    public PlatformTransactionManager transactionManager(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactoryPrimary(builder).getObject());
    }

    private Map<String, Object> getHibernateProperties() {
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
    }

}
```

从数据源配置类

```java
package com.icoderoad.example.datasource.conf;

import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Description: 从数据源配置
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactorySecond",
                        transactionManagerRef = "transactionManagerSecond",
                        basePackages = {"com.icoderoad.example.datasource.repository.secondary"})
public class SecondaryDataSourceConfig {

    @Autowired
    @Qualifier("secondDataSource")
    private DataSource secondDataSource;

    @Resource
    private JpaProperties jpaProperties;

    @Resource
    private HibernateProperties hibernateProperties;

    @Bean(name = "entityManagerSecond")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return entityManagerFactorySecond(builder).getObject().createEntityManager();
    }

    @Bean(name = "entityManagerFactorySecond")    //从实体工厂
    public LocalContainerEntityManagerFactoryBean entityManagerFactorySecond (EntityManagerFactoryBuilder builder) {

        return builder.dataSource(secondDataSource)
                .properties(getHibernateProperties())
                .packages("com.icoderoad.example.datasource.entity.secondary")     //换成你自己的实体类所在位置
                .persistenceUnit("secondaryPersistenceUnit")
                .build();
    }

    @Bean(name = "transactionManagerSecond")
    public PlatformTransactionManager transactionManager(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactorySecond(builder).getObject());
    }

    private Map<String, Object> getHibernateProperties() {
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
    }

}
```

创建实体类表示用户，用于JPA映射：

```java
//主数据源实体类
package com.icoderoad.example.datasource.entity.primary;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "datasource_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    
}
//从数据源实体类
package com.icoderoad.example.datasource.entity.secondary;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "datasource_users")
public class SecUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    
}
```

创建两个数据源的Repository接口，用于数据库操作：

```java
package com.icoderoad.example.datasource.repository.primary;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.datasource.entity.primary.User;

//主数据源Repository
public interface PrimaryUserRepository extends JpaRepository<User, Long> {
}

package com.icoderoad.example.datasource.repository.secondary;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.datasource.entity.secondary.SecUser;

//第二个数据源Repository
public interface SecondaryUserRepository extends JpaRepository<SecUser, Long> {
}
```

初始化数据，例如在启动类中：

```java
package com.icoderoad.example.datasource;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.icoderoad.example.datasource.entity.primary.User;
import com.icoderoad.example.datasource.entity.secondary.SecUser;
import com.icoderoad.example.datasource.repository.primary.PrimaryUserRepository;
import com.icoderoad.example.datasource.repository.secondary.SecondaryUserRepository;

@SpringBootApplication
public class DatasourceApplication {
    @Autowired
    private PrimaryUserRepository primaryUserRepository;

    @Autowired
    private SecondaryUserRepository secondaryUserRepository;

    public static void main(String[] args) {
        SpringApplication.run(DatasourceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // 初始化主数据源用户
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUsername("主数据源用户 " + i);
            user.setEmail("primaryuser" + i + "@example.com");
            primaryUserRepository.save(user);
        }

        // 初始化第二个数据源用户
        for (int i = 1; i <= 10; i++) {
        	SecUser user = new SecUser();
            user.setUsername("从数据源用户 " + i);
            user.setEmail("secondaryuser" + i + "@example.com");
            secondaryUserRepository.save(user);
        }
    }
}
```

Controller类

```java
package com.icoderoad.example.datasource.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.datasource.entity.primary.User;
import com.icoderoad.example.datasource.entity.secondary.SecUser;
import com.icoderoad.example.datasource.repository.primary.PrimaryUserRepository;
import com.icoderoad.example.datasource.repository.secondary.SecondaryUserRepository;

@Controller
public class UserController {
    @Autowired
    private PrimaryUserRepository primaryUserRepository;

    @Autowired
    private SecondaryUserRepository secondaryUserRepository;

    @GetMapping("/")
    public String showUsers(Model model) {
        List<User> primaryUsers = primaryUserRepository.findAll();
        List<SecUser> secondaryUsers = secondaryUserRepository.findAll();

        model.addAttribute("primaryUsers", primaryUsers);
        model.addAttribute("secondaryUsers", secondaryUsers);

        return "user-list"; // 返回Thymeleaf模板名称
    }
}
```



创建Thymeleaf模板 user-list.html 以显示用户数据

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
    <!-- 引入Bootstrap的CSS文件 -->
	<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0/dist/css/bootstrap.min.css">

</head>
<body>
    <div class="container">
        <!-- 显示主数据源用户 -->
        <h2>主数据源用户</h2>
        <table class="table">
            <!-- 表头... -->
            <tbody>
                <tr th:each="user : ${primaryUsers}">
                    <td th:text="${user.id}"></td>
                    <td th:text="${user.username}"></td>
                    <td th:text="${user.email}"></td>
                </tr>
            </tbody>
        </table>

        <!-- 显示第二个数据源用户 -->
        <h2>第二个数据源用户</h2>
        <table class="table">
            <!-- 表头... -->
            <tbody>
                <tr th:each="user : ${secondaryUsers}">
                    <td th:text="${user.id}"></td>
                    <td th:text="${user.username}"></td>
                    <td th:text="${user.email}"></td>
                </tr>
            </tbody>
        </table>
    </div>
</body>
</html>
```

启动应用程序：

运行Spring Boot应用程序，访问http://localhost:8080/，会显示主数据源及第二个数据源的10条用户数据。效果如下 图：
