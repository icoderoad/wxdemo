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
    
    @Scheduled(cron = "0 36 14 * * ?")
    public void cronTask() {
        System.out.println("定时任务已执行");
        taskExecutor.execute(() -> asyncService.asyncTask("定时任务"));
    }

}