package com.lian.lianojbackenduserservice.aop;

import com.lian.lianojbackendcommon.annotation.AuthCheck;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.constant.UserConstant;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendmodel.model.entity.User;
import com.lian.lianojbackenduserservice.service.UserService;
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
 * <p>与 question-service 的 AuthInterceptor 同构：使控制器上失效的
 * {@code @AuthCheck} 注解真正生效。区别在于本服务就是用户服务的提供方，
 * 直接使用本地 {@link UserService} 读取登录用户，无需经过 Feign。
 *
 * @author lian
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截：方法标注 @AuthCheck 且指定了角色时，校验当前登录用户的角色
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        if (StringUtils.isNotBlank(mustRole)) {
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                User loginUser = userService.getLoginUser(request);
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
