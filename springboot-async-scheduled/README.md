SpringBoot 使用异步@Async 、定时@Scheduled 注解及线程池执行异步定时任务

@Async 注解用于标记方法，表示该方法应该在单独的线程中异步执行，从而不会阻塞调用线程。为了使用@Async，需要进行以下配置：

在主应用程序类上添加@EnableAsync 注解，以启用异步处理。
在要异步执行的方法上添加@Async 注解。

@Scheduled 注解：
@Scheduled 注解用于创建定时任务。以下是@Scheduled 注解的常用属性：

fixedRate: 以固定的速率执行任务，任务开始时间为上一次任务的结束时间。
fixedDelay: 以固定的延迟执行任务，任务开始时间为上一次任务的开始时间加上延迟时间。
initialDelay: 初始延迟，任务第一次执行前的延迟时间。
cron: 使用Cron表达式指定更复杂的定时规则。

当使用 @Scheduled 注解的 cron 属性时，你可以使用Cron表达式来定义更灵活和复杂的定时任务规则。Cron表达式由6个字段组成，分别表示秒、分钟、小时、月份中的日期、月份、星期中的日期。每个字段都可以包含一个具体的值、一组值、范围、通配符或者使用逗号分隔的值。

下面是Cron表达式的详细用法说明：

秒（Seconds）： 取值范围是0-59。

分钟（Minutes）： 取值范围是0-59。

小时（Hours）： 取值范围是0-23。

月份中的日期（Day of month）： 取值范围是1-31。

月份（Months）： 取值范围是1-12，或者可以使用缩写的月份名称（比如：JAN、FEB、MAR等）。

星期中的日期（Day of week）： 取值范围是0-6，0 表示星期日，1 表示星期一，依此类推。也可以使用缩写的星期名称（比如：SUN、MON、TUE等）。

Cron表达式的格式为：秒 分 时 日 月 星期。以下是一些常用的Cron表达式示例：

每天的上午10点执行：0 0 10 * * ?
每小时的30分钟执行：0 30 * * * ?
每天的中午12点和下午6点执行：0 0 12,18 * * ?
每个工作日的上午9点到下午5点之间，每隔半小时执行：0 0/30 9-17 * * MON-FRI
每个月的第一天凌晨1点执行：0 0 1 1 * ?
每周五的下午3点执行：0 0 15 ? * FRI
每年的7月份和12月份的第一天凌晨2点执行：0 0 2 1 7,12 ?
注意事项：

使用 ? 表示不关心（不指定）的值，比如在月份中的日期和星期中的日期字段中，可以使用 ?。

* 表示所有可能的值，比如在小时字段中的 * 表示每个小时都执行。
  / 表示递增，比如在分钟字段中的 0/15 表示每隔15分钟执行一次。
  , 用于列出一个清单的值，比如在星期字段中的 MON,WED,FRI 表示星期一、三和五都执行。

 ThreadPoolTaskExecutor 提供了一系列的属性来配置线程池的行为。以下是一些常用的属性：

corePoolSize：核心线程数，线程池会保持这些线程即使没有任务需要执行。
maxPoolSize：最大线程数，线程池能创建的最大线程数。
queueCapacity：任务队列容量，如果所有的核心线程都在工作并且队列已满，新任务会在队列中等待。
keepAliveSeconds：线程空闲时间，超过这个时间空闲的线程会被销毁。
threadNamePrefix：线程名前缀，用于区分不同线程的标识。
rejectedExecutionHandler：拒绝策略，定义了当任务被拒绝时的处理方式。

当结合异步任务、定时任务、线程池以及异常处理时，我们可以按照以下步骤进行配置。下面是一个完整的示例，包含了异步定时任务的配置，线程池的配置，异常处理，以及注释说明。

创建一个Spring Boot项目，并在pom.xml文件中添加必要的依赖：

```xml
`<!-- Spring Boot Starter -->`
`<dependency>`
    `<groupId>org.springframework.boot</groupId>`
    `<artifactId>spring-boot-starter</artifactId>`
`</dependency>`

`<!-- Spring Boot Starter Web -->`
`<dependency>`
    `<groupId>org.springframework.boot</groupId>`
    `<artifactId>spring-boot-starter-web</artifactId>`
`</dependency>`

`<!-- Spring Boot Starter AOP -->`
`<dependency>`
    `<groupId>org.springframework.boot</groupId>`
    `<artifactId>spring-boot-starter-aop</artifactId>`
`</dependency>
```

创建异步定时任务的配置类，同时配置线程池和异常处理：

```java
package com.icoderoad.example.AsyncScheduled.conf;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncScheduledConfig implements AsyncConfigurer, SchedulingConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("TaskAsync-");
        executor.initialize();
        return executor;
    }
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
    }
    
    @Bean(destroyMethod = "shutdown")
    public Executor taskScheduler() {
        return Executors.newScheduledThreadPool(10);
    }

}
```

创建异步任务的服务类，其中包含一个异步方法：

```java
package com.icoderoad.example.AsyncScheduled.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {
    
    @Async
    public void asyncTask(String taskName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String threadName = Thread.currentThread().getName();
        System.out.println("异步任务 '" + taskName + "'执行时间: " + dateFormat.format(new Date()) +
                " 线程名: " + threadName);
        try {
            // 模拟耗时操作
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("异步任务 '" + taskName + "' 完成时间 " + dateFormat.format(new Date()) +
                " 线程名: " + threadName);
    }

}
```

创建定时任务的服务类，其中包含多个定时任务方法：

```java
package com.icoderoad.example.AsyncScheduled.service;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasks {

	@Autowired
	private Executor taskExecutor ;
	
	@Autowired
	private AsyncService asyncService;
	
    @Scheduled(fixedRate = 5000)
    public void fixedRateTask() {
        System.out.println("固定速率任务已执行");
        taskExecutor.execute(() -> asyncService.asyncTask("固定速率任务"));
    }
    
    @Scheduled(fixedDelay = 10000)
    public void fixedDelayTask() {
        System.out.println("固定延迟任务已执行");
        taskExecutor.execute(() -> asyncService.asyncTask("固定延迟任务"));
    }
    
    @Scheduled(initialDelay = 3000, fixedRate = 60000)
    public void initialDelayTask() {
        System.out.println("初始延迟任务已执行");
        taskExecutor.execute(() -> asyncService.asyncTask("初始延迟任务"));
    }
    
    @Scheduled(cron = "0 0 11 * * ?")
    public void cronTask() {
        System.out.println("定时任务已执行");
        taskExecutor.execute(() -> asyncService.asyncTask("定时任务"));
    }

}
```

在主应用程序类中启动Spring Boot应用：

```java
package com.icoderoad.example.AsyncScheduled;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AsyncScheduledApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsyncScheduledApplication.class, args);
	}

}
```

上述示例展示了如何配置Spring Boot项目以支持异步定时任务，包括配置线程池、创建异步方法、定时任务方法以及异常处理。在运行该应用时，我们将看到异步任务和定时任务按照预定规则执行，并且异常情况得到适当处理。

启动应用服务 AsyncScheduledApplication，在控制台会按顺序打印如下信息：

固定速率任务已执行

异步任务 '固定速率任务'执行时间: 2023-08-29 14:48:47 线程名: TaskAsync-2

固定延迟任务已执行

异步任务 '固定延迟任务'执行时间: 2023-08-29 14:48:47 线程名: TaskAsync-4

异步任务 '固定速率任务' 完成时间 2023-08-29 14:48:49 线程名: TaskAsync-2

异步任务 '固定延迟任务' 完成时间 2023-08-29 14:48:49 线程名: TaskAsync-4

初始延迟任务已执行

异步任务 '初始延迟任务'执行时间: 2023-08-29 14:48:50 线程名: TaskAsync-6

固定速率任务已执行

要想让定时任务执行，需要将定时任务时间调整为启动任务之后的时间即可。