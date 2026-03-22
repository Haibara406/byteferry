package com.byteferry.byteferry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheInvalidationService cacheInvalidationService;

    private static final String CACHE_INVALIDATION_STREAM = "cache:invalidation:stream";
    private static final String CONSUMER_GROUP = "cache-invalidation-group";
    private static final String CONSUMER_NAME = "consumer-" + UUID.randomUUID();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void startConsuming() {
        // 创建消费者组（如果不存在）
        try {
            redisTemplate.opsForStream().createGroup(CACHE_INVALIDATION_STREAM, CONSUMER_GROUP);
            log.info("Created consumer group: {}", CONSUMER_GROUP);
        } catch (Exception e) {
            // 组已存在，忽略错误
            log.debug("Consumer group already exists: {}", CONSUMER_GROUP);
        }

        running.set(true);
        executorService.submit(this::consumeMessages);
        log.info("Started cache invalidation consumer: {}", CONSUMER_NAME);
    }

    @PreDestroy
    public void stopConsuming() {
        running.set(false);
        executorService.shutdown();
        log.info("Stopped cache invalidation consumer: {}", CONSUMER_NAME);
    }

    private void consumeMessages() {
        while (running.get()) {
            try {
                // 从消费者组读取消息
                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                        StreamOffset.create(CACHE_INVALIDATION_STREAM, ReadOffset.lastConsumed())
                );

                if (messages != null && !messages.isEmpty()) {
                    for (MapRecord<String, Object, Object> message : messages) {
                        processMessage(message);
                        // 确认消息
                        redisTemplate.opsForStream().acknowledge(CACHE_INVALIDATION_STREAM, CONSUMER_GROUP, message.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Error consuming cache invalidation messages", e);
                try {
                    Thread.sleep(1000); // 出错后等待1秒再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processMessage(MapRecord<String, Object, Object> message) {
        try {
            Map<Object, Object> value = message.getValue();
            String cacheType = (String) value.get("cacheType");
            List<Integer> userIdInts = (List<Integer>) value.get("userIds");

            // 转换 Integer 到 Long
            Set<Long> userIds = new HashSet<>();
            if (userIdInts != null) {
                for (Integer userId : userIdInts) {
                    userIds.add(userId.longValue());
                }
            }

            log.debug("Processing cache invalidation: type={}, userIds={}", cacheType, userIds);

            // 执行本地缓存失效
            cacheInvalidationService.invalidateCacheLocally(cacheType, userIds);

        } catch (Exception e) {
            log.error("Error processing cache invalidation message: {}", message, e);
        }
    }
}