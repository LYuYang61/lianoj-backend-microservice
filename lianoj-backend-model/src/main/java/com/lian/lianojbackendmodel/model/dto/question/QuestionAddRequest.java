package com.lian.lianojbackendmodel.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author lian
 * @title QuestionAddRequest
 * @date 2025/1/10 10:41
 * @description 题目添加请求
 */
@Data
public class QuestionAddRequest implements Serializable {
    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 题目答案
     */
    private String answer;

    /**
     * 判题用例
     */
    private List<JudgeCase> judgeCase;

    /**
     * 判题配置
     */
    private JudgeConfig judgeConfig;

    /**
     * SPJ 特殊判题校验程序（E10，可空）：
     * 仅 judgeConfig.spjMode=special 时使用，Java 程序，协议见 Question.spjCode 注释
     */
    private String spjCode;

    private static final long serialVersionUID = 1L;

}
