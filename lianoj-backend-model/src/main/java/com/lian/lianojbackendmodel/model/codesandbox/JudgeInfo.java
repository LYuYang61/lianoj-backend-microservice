package com.lian.lianojbackendmodel.model.codesandbox;

import lombok.Data;

/**
 * @author lian
 * @title JudgeInfo
 * @date 2025/1/10 15:46
 * @description 判题信息
 */
@Data
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗内存
     */
    private Long memory;

    /**
     * 消耗时间（KB）
     */
    private Long time;

    /**
     * 得分（E10 赛制判题）：
     * OI 赛制 = 通过用例的分值累加；ACM 赛制 = 全部通过时为总分，否则 0。
     * 未配置用例分值（传统题目）时为 null，前端展示时降级为 Accepted/WA 文案。
     */
    private Long score;
}
