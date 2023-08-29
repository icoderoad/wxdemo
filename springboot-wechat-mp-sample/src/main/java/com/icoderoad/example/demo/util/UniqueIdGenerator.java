package com.icoderoad.example.demo.util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UniqueIdGenerator {

    private static final String LOCK_KEY = "id_generation_lock";

    private final SnowflakeIdGenerator idGenerator;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${worker.id}")
    private long workerId;

    public UniqueIdGenerator() {
        this.idGenerator = new SnowflakeIdGenerator(workerId);
    }

    public long generateUniqueId() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            lock.lock();
            return idGenerator.generateId();
        } finally {
            lock.unlock();
        }
    }
}