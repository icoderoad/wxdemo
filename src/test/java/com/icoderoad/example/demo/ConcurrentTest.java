package com.icoderoad.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "server.port=8080")
public class ConcurrentTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executorService.execute(() -> {
                String response = restTemplate.getForObject("http://localhost:8080/limitApi", String.class);
                System.out.println(response);
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}