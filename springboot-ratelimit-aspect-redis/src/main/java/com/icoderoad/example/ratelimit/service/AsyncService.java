package com.icoderoad.example.ratelimit.service;

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