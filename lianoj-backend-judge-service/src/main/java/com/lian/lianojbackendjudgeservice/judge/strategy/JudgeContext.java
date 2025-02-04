package com.lian.lianojbackendjudgeservice.judge.strategy;


import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;
import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * @author lian
 * @title JudgeContext
 * @date 2025/1/14 10:42
 * @description 判题上下文（用于定义在策略中传递的参数）
 */
@Data
public class JudgeContext {
    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;

    /**
     * 输入数据
     */
    private List<String> inputList;

    /**
     * 输出数据
     */
    private List<String> outputList;

    /**
     * 判题用例
     */
    private List<JudgeCase> judgeCaseList;

    /**
     * 题目
     */
    private Question question;

    /**
     * 问题目提交
     */
    private QuestionSubmit questionSubmit;
}
