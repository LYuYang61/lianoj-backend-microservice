package com.lian.lianojbackendmodel.model.codesandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lian
 * @title ExecuteCodeResponse
 * @date 2025/1/13 23:02
 * @description 执行代码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 执行状态：用例全部执行完成（无论判定对错，对错由判题策略比对得出）
     */
    public static final int STATUS_SUCCESS = 1;

    /**
     * 执行状态：沙箱内部错误（如代码命中危险关键字黑名单、环境异常）
     */
    public static final int STATUS_SANDBOX_ERROR = 2;

    /**
     * 执行状态：用户代码运行出错（如运行时异常）
     */
    public static final int STATUS_RUN_ERROR = 3;

    private List<String> outputList;

    /**
     * 接口信息
     */
    private String message;

    /**
     * 执行状态
     */
    private Integer status;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;
}
