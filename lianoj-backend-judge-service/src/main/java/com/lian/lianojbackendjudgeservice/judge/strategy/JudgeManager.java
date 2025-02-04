package com.lian.lianojbackendjudgeservice.judge.strategy;


import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * @author lian
 * @title JudgeManager
 * @date 2025/1/14 10:49
 * @description 判题管理器（简化判题策略的使用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}
