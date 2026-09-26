package com.lian.lianojbackendjudgeservice.controller;

import com.lian.lianojbackendcommon.annotation.AuthCheck;
import com.lian.lianojbackendcommon.common.BaseResponse;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.common.ResultUtils;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendjudgeservice.judge.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author lian
 * @title RejudgeController
 * @description 判题管理接口（E07 管理端重判）
 *
 * <p>路径说明：网关按 Path=/api/judge/** 路由到本服务（context-path=/api/judge），
 * 本接口完整路径为 /api/judge/rejudge，仅管理员可调用（@AuthCheck 切面校验）。
 * 对应前端入口：提交记录页的管理员"重判"按钮。
 */
@RestController
@RequestMapping("/")
@Slf4j
public class RejudgeController {

    @Resource
    private JudgeService judgeService;

    /**
     * 重判一次已完成的提交
     *
     * @param questionSubmitId 提交 id
     * @return true = 已重新入队
     */
    @PostMapping("/rejudge")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> rejudge(@RequestParam("questionSubmitId") long questionSubmitId) {
        if (questionSubmitId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = judgeService.rejudge(questionSubmitId);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "重判失败：提交不存在或正在判题中");
        }
        return ResultUtils.success(true);
    }
}
