package com.lian.lianojbackendcommon.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 限流计数器（编程式用法）
 *
 * <p>基于 Redis 的固定窗口计数法：以 INCR 计数、首次访问时设置过期时间，
 * 两者通过 Lua 脚本原子执行，避免"先 INCR 后 EXPIRE"在两步之间宕机
 * 导致计数键永不过期（演变为永久限流）的竞态问题。
 *
 * <p>声明式入口见 {@link com.lian.lianojbackendcommon.aop.RateLimitAspect}，
 * 本类是计数逻辑的唯一实现点；限流维度需要在方法内部解析（如登录用户 id）时，
 * 直接注入本类编程式调用。
 *
 * @author lian
 */
@Component
@Slf4j
public class RateLimiter {

    /**
     * 统一的键前缀，避免与其他业务的 Redis 键冲突（本项目 Redis database 1 同时存放会话数据）
     */
    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * 原子窗口计数脚本：
     * KEYS[1] = 计数键；ARGV[1] = 窗口秒数
     * 返回当前窗口内的累计次数
     */
    private static final DefaultRedisScript<Long> WINDOW_COUNT_SCRIPT;

    static {
        WINDOW_COUNT_SCRIPT = new DefaultRedisScript<>();
        WINDOW_COUNT_SCRIPT.setScriptText(
                "local count = redis.call('incr', KEYS[1]) " +
                "if count == 1 then " +
                "    redis.call('expire', KEYS[1], ARGV[1]) " +
                "end " +
                "return count");
        WINDOW_COUNT_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消费一次限额
     *
     * @param key          限流键（不含前缀，如 "question_submit:user:1"）
     * @param windowSeconds 时间窗口（秒）
     * @param maxCount     窗口内允许的最大次数
     * @return true = 放行；false = 已超限
     */
    public boolean tryConsume(String key, int windowSeconds, int maxCount) {
        String redisKey = KEY_PREFIX + key;
        Long count;
        try {
            count = stringRedisTemplate.execute(
                    WINDOW_COUNT_SCRIPT, Collections.singletonList(redisKey), String.valueOf(windowSeconds));
        } catch (Exception e) {
            // 降级策略：Redis 故障时放行（fail-open）。
            // 理由：Redis 同时是全站会话存储，其故障时用户根本无法登录到达本接口，
            // 限流器单独 fail-closed 只会放大故障面；此处保留 error 日志便于定位。
            log.error("限流计数异常, 降级放行: key={}", redisKey, e);
            return true;
        }
        long current = count == null ? 0L : count;
        if (current == 1L) {
            // count==1 说明是窗口起点，顺带记录便于排查
            log.debug("限流窗口开启: key={}, window={}s, max={}", redisKey, windowSeconds, maxCount);
        }
        return current <= maxCount;
    }

    /**
     * 组合键辅助方法：多维度限流时拼接键（如 用户维度+接口维度）
     */
    public static String buildKey(List<String> dimensions) {
        return String.join(":", dimensions);
    }
}
