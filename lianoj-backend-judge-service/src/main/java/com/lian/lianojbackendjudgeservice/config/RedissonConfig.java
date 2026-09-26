package com.lian.lianojbackendjudgeservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置（E08 分布式锁）
 *
 * <p>为什么不用 redisson-spring-boot-starter：starter 内置的 spring-data 适配模块
 * （redisson-spring-data-xx）与 Spring Boot 版本存在矩阵耦合，Boot 2.6.13 需要精确匹配
 * redisson-spring-data-26，装错版本会启动报错。本工程只使用 RedissonClient 本体（分布式锁），
 * 手写 SingleServer 配置依赖面最小、行为最可控。
 *
 * <p>连接参数直接复用 spring.redis.*（与 Spring Session 同一个 Redis 实例），
 * 注意 redisson 的地址格式要求带协议前缀 redis://。
 *
 * @author lian
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.password}") String password,
            @Value("${spring.redis.database:0}") int database) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password)
                .setDatabase(database)
                // 判题是短事务：锁默认持有 30 秒（tryLock 显式传入），
                // 看门狗（watchdog）会在业务超期时自动续期，防止锁提前释放
                .setConnectTimeout(3000)
                .setTimeout(3000);
        return Redisson.create(config);
    }
}
