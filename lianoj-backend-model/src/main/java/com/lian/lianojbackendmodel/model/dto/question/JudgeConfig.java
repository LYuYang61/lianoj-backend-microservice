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

    /**
     * 判题模式（E10，可空，默认 standard）：
     * standard = 精确比对（输出与期望完全一致）；
     * special  = SPJ 特殊判题（由题目的 spjCode 校验程序判定该用例是否通过）
     */
    private String spjMode;

    /**
     * 赛制（E10，可空，默认 acm）：
     * acm = 任何用例错误即 0 分（竞赛模式）；
     * oi  = 按通过用例的分值累加（部分得分）
     */
    private String judgeMode;
}
