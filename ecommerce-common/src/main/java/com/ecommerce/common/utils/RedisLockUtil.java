package com.ecommerce.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 */
@Component
public class RedisLockUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final Long RELEASE_SUCCESS = 1L;

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的键
     * @param expireTime 过期时间（秒）
     * @return 锁的唯一标识（用于释放锁）
     */
    public String tryLock(String lockKey, long expireTime) {
        String uuid = UUID.randomUUID().toString();
        String key = LOCK_PREFIX + lockKey;
        
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, uuid, expireTime, TimeUnit.SECONDS);
        
        return Boolean.TRUE.equals(success) ? uuid : null;
    }

    /**
     * 释放分布式锁（使用Lua脚本保证原子性）
     * @param lockKey 锁的键
     * @param lockValue 锁的唯一标识
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey, String lockValue) {
        String key = LOCK_PREFIX + lockKey;
        
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(redisScript, 
                Collections.singletonList(key), lockValue);
        
        return RELEASE_SUCCESS.equals(result);
    }

    /**
     * 尝试获取锁（带重试）
     * @param lockKey 锁的键
     * @param expireTime 过期时间（秒）
     * @param retryCount 重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @return 锁的唯一标识
     */
    public String tryLockWithRetry(String lockKey, long expireTime, 
                                    int retryCount, long retryInterval) {
        String lockValue = null;
        for (int i = 0; i < retryCount; i++) {
            lockValue = tryLock(lockKey, expireTime);
            if (lockValue != null) {
                return lockValue;
            }
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }
}
