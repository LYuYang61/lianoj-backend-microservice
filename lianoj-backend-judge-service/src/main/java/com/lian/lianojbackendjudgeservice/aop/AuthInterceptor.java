package com.lian.lianojbackendjudgeservice.aop;

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
 * 权限校验切面（判题服务，与 question-service 的 AuthInterceptor 同构）
 *
 * <p>使本服务控制器（如 RejudgeController.rejudge）上的 {@code @AuthCheck} 注解生效。
 * 登录用户信息来自 Redis 共享会话（UserFeignClient.getLoginUser 为 default 方法，
 * 本地读取 session，不发起 Feign 请求）。
 *
 * @author lian
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserFeignClient userFeignClient;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        if (StringUtils.isNotBlank(mustRole)) {
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            User loginUser = userFeignClient.getLoginUser(request);
            String userRole = loginUser.getUserRole();
            // 角色校验：管理员放行一切；否则必须与要求角色精确匹配
            if (!UserConstant.ADMIN_ROLE.equals(userRole) && !mustRole.equals(userRole)) {
                log.warn("权限校验失败: userId={}, userRole={}, 需要角色={}",
                        loginUser.getId(), userRole, mustRole);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        return joinPoint.proceed();
    }
}
