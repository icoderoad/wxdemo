package com.icoderoad.example.kaforder.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.icoderoad.example.kaforder.service.TestDataProducerService;

@Component
public class TestDataGenerationRunner implements ApplicationRunner {

    private final TestDataProducerService testDataProducer;

    public TestDataGenerationRunner(TestDataProducerService testDataProducer) {
        this.testDataProducer = testDataProducer;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 在应用程序启动后调用生成测试订单数据方法
        testDataProducer.produceTestData(3); // 生成1万条测试数据
    }
}
