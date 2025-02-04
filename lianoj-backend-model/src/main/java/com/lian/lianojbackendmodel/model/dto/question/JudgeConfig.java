package com.lian.lianojbackendmodel.model.dto.question;

import lombok.Data;

/**
 * @author lian
 * @title JudgeConfig
 * @date 2025/1/10 10:40
 * @description 判题配置
 */
@Data
public class JudgeConfig {
    /**
     * 时间限制（ms）
     */
    private Long timeLimit;

    /**
     * 内存限制（KB）
     */
    private Long memoryLimit;

    /**
     * 堆栈限制（KB）
     */
    private Long stackLimit;
}
