package com.lian.lianojbackendcommon.aop;

import com.lian.lianojbackendcommon.annotation.RateLimit;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendcommon.utils.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * 限流切面（声明式用法）
 *
 * <p>拦截 {@link RateLimit} 注解，解析 key 中的 SpEL（可引用方法参数）得到限流键，
 * 委托 {@link RateLimiter} 完成原子窗口计数，超限抛出业务异常。
 *
 * @author lian
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /**
     * 参数名发现器：优先读 -parameters 编译参数，回退读 LocalVariableTable 调试信息
     * （Maven 默认 debug=true 包含该信息）。不直接用 reflection Parameter.getName()——
     * 未加 -parameters 时它只会返回 arg0/arg1，导致 SpEL 中 #参数名 静默取不到值。
     */
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    @Resource
    private RateLimiter rateLimiter;

    /**
     * 环绕拦截：计算限流键 → 消费限额 → 放行或抛异常
     */
    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = parseKey(rateLimit.key(), joinPoint);
        boolean allowed = rateLimiter.tryConsume(key, rateLimit.window(), rateLimit.maxCount());
        if (!allowed) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
        return joinPoint.proceed();
    }

    /**
     * 解析 SpEL 限流键：以方法参数名作为上下文变量（如 #id）
     */
    private String parseKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        Expression expression = SPEL_PARSER.parseExpression(keyExpression);
        Object value = expression.getValue(context);
        return value == null ? "unknown" : value.toString();
    }
}
