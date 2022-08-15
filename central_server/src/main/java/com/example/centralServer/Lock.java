package com.example.centralServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Lock {
    @Resource
    private RedisLockRegistry redisLockRegistry;
    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping("/lock")
    public boolean getLock(String lockKey, String identity, long expireTime) {
        try {
            boolean lockResult = redisTemplate.opsForValue().setIfAbsent(lockKey, identity, expireTime, TimeUnit.SECONDS);
            return lockResult;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @GetMapping("/lock")
    public Object unlock(String lockKey, String identity) {
        String luaScript = "if " +
                "  redis.call('get', KEYS[1]) == ARGV[1] " +
                "then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";

        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Boolean.class);
        redisScript.setScriptText(luaScript);
        List<String> keys = new ArrayList<>();
        keys.add(lockKey);
        Object result = redisTemplate.execute(redisScript, keys, identity);
        return (boolean) result;
    }
}
