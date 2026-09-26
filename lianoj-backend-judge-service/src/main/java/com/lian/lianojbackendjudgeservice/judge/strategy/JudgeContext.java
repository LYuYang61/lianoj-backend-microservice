package com.lian.lianojbackendjudgeservice.judge.strategy;


import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
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

    /**
     * 沙箱引用（E10）：SPJ 特殊判题的比对器需要在策略内部再次调用沙箱执行校验程序，
     * 通过上下文把判题服务已构建好的（经代理包装的）沙箱实例传进来，避免策略层重新装配
     */
    private CodeSandbox codeSandbox;
}
