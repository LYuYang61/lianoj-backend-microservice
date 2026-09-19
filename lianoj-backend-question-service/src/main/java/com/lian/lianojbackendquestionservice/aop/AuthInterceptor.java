package com.lian.lianojbackendquestionservice.aop;

import com.lian.lianojbackendcommon.annotation.AuthCheck;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.constant.UserConstant;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendmodel.model.entity.User;
import com.lian.lianojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验切面（P0 补洞）
 *
 * <p>背景：控制器上的 {@code @AuthCheck(mustRole = "admin")} 注解在本工程中
 * 原本没有任何切面处理——注解形同虚设，管理端接口（如题目更新）实际对
 * 所有登录用户开放。本切面使注解真正生效。
 *
 * <p>实现说明：登录用户信息来自 Redis 共享会话（见 UserFeignClient.getLoginUser
 * 的 default 实现——各服务共享同一 SESSION，本地即可读取，无需发起 Feign 请求）。
 *
 * @author lian
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 执行拦截：方法标注 @AuthCheck 且指定了角色时，校验当前登录用户的角色
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        if (StringUtils.isNotBlank(mustRole)) {
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            User loginUser = userFeignClient.getLoginUser(request);
            String userRole = loginUser.getUserRole();
            // 角色校验：管理员放行一切；否则必须与要求角色精确匹配
            // （不能只判断 mustRole==admin——否则 mustRole 为其他值时会被静默放行）
            if (!UserConstant.ADMIN_ROLE.equals(userRole) && !mustRole.equals(userRole)) {
                log.warn("权限校验失败: userId={}, userRole={}, 需要角色={}",
                        loginUser.getId(), userRole, mustRole);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        return joinPoint.proceed();
    }
}
