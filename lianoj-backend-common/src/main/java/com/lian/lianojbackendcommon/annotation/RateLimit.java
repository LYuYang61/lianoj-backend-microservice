package com.lian.lianojbackendcommon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解（声明式用法）
 *
 * <p>配合 {@link com.lian.lianojbackendcommon.aop.RateLimitAspect} 使用：
 * 以 Redis 窗口计数法实现"时间窗口内最多 maxCount 次"的访问限制，超限抛出
 * {@link com.lian.lianojbackendcommon.common.ErrorCode#TOO_MANY_REQUEST}。
 *
 * <p>key 支持 SpEL 表达式，可引用方法参数（按参数名）。
 * 适用场景：限流维度可以由方法参数直接表达的接口；
 * 若限流维度（如登录用户 id）需要在方法内部才能解析，请改用
 * {@link com.lian.lianojbackendcommon.utils.RateLimiter} 编程式调用。
 *
 * <p>示例：{@code @RateLimit(key = "'question:detail:' + #id", window = 60, maxCount = 100)}
 *
 * @author lian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流键（SpEL 表达式，支持引用方法参数），实际存入 Redis 时会加上统一前缀
     */
    String key();

    /**
     * 时间窗口大小（秒）
     */
    int window() default 60;

    /**
     * 时间窗口内允许的最大访问次数
     */
    int maxCount() default 100;

    /**
     * 超限提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
