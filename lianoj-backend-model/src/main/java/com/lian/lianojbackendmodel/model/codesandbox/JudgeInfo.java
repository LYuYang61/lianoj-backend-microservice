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
}
