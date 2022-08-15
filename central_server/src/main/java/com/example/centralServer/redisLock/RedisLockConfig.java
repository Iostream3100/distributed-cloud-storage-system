//package com.example.centralServer.redisLock;
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.integration.redis.util.RedisLockRegistry;
//
//public class RedisLockConfig {
//    @Bean
//    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
//        return new RedisLockRegistry(redisConnectionFactory, "my-lock");
//    }
//}
